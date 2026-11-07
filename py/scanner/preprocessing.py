### PRÉ-PROCESSAMENTO OPENCV = OPERA EM MEMORIA SEM GRAVAR OS ARQUIVOS ###

import io

import cv2
import numpy as np 
from PIL import Image, ImageOps

# EVITA "BOMBAS DE DESCOMPRESSÃO": ACIMA ~100 MP A PILLOW LEVANTA ERRO
Image.MAX_IMAGE_PIXELS = 50_000_000

def carregar_imagem(dados: bytes):
    # BYTES -> IMAGEM BGR, COM ROTAÇÃO JÁ CORRIGIDA NO EXIF (FOTOS DE CELULAR)
    img = Image.open(io.BytesIO(dados))
    img = ImageOps.exif_transpose(img).convert("RGB")
    return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)

def para_cinza(img):
    return cv2.cvtColor(img, cv2.COLOR_BGR2GRAY) if img.ndim == 3 else img

def melhorar_contraste(cinza):
    return cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8)).apply(cinza)

def reduzir_ruido(cinza):
    return cv2.medianBlur(cinza, 3)

def binarizar(cinza):
    return cv2.adaptiveThreshold(cinza, 255, cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
                                 cv2.THRESH_BINARY, 31, 15)

def ordenar_fts(pts):
    pts = pts.reshape(4, 2).astype("float32")
    soma, dif = pts.sum(axis=1), np.diff(pts, axis=1).ravel()
    return np.array([pts[np.argmin(soma)], pts[np.argmin(dif)],
                     pts[np.argmax(soma)], pts[np.argmax(dif)]], dtype="float32")

def corrigir_perspectiva(img):
    # Acha o maior quadrilátero (a folha) e o endireita.
    # Se não achar uma folha convincente (>25% da imagem), 
    # devolve a imagem original.
    cinza = reduzir_ruido(para_cinza(img))
    bordas = cv2.dilate(cv2.Canny(cinza, 50, 150), None, iterations=1)
    contornos, _ = cv2.findContours(bordas, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)
    area_img = img.shape[0] * img.shape[1]
    for c in sorted(contornos, key=cv2.contourArea, reverse=True)[:5]:
        aprox = cv2.approxPolyDP(c, 0.02 * cv2.arcLength(c, True), True)
        if len(aprox) == 4 and cv2.contourArea(aprox) > 0.25 * area_img:
            tl, tr, br, bl = _ordenar_pontos(aprox)
            w = int(max(np.linalg.norm(br - bl), np.linalg.norm(tr - tl)))
            h = int(max(np.linalg.norm(tr - br), np.linalg.norm(tl - bl)))
            destino = np.array([[0, 0], [w - 1, 0], [w - 1, h - 1], [0, h - 1]], dtype="float32")
            m = cv2.getPerspectiveTransform(np.array([tl, tr, br, bl], dtype="float32"), destino)
            return cv2.warpPerspective(img, m, (w, h))
    return img


def preparar_para_ocr(img):
    # Cinza -> contraste -> ruído -> binarização adaptativa.
    return binarizar(reduzir_ruido(melhorar_contraste(para_cinza(img))))


# Faixas HSV do OpenCV (H vai de 0 a 179). Vermelho dá a volta no círculo, por isso são duas.
FAIXAS_COR = {
    "vermelho": [((0, 70, 50), (10, 255, 255)), ((170, 70, 50), (179, 255, 255))],
    "verde": [((40, 70, 50), (85, 255, 255))],
    "azul": [((100, 70, 50), (130, 255, 255))],
}


def cor_predominante(img, caixa=None):
    # Cor com mais pixels na região (x0, y0, x1, y1). Devolve None se nenhuma passa de 2%.
    if caixa:
        x0, y0, x1, y1 = caixa
        img = img[max(0, y0):y1, max(0, x0):x1]
    if img.size == 0:
        return None
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    melhor, maior = None, 0
    for nome, faixas in FAIXAS_COR.items():
        mascara = np.zeros(hsv.shape[:2], dtype="uint8")
        for lo, hi in faixas:
            mascara |= cv2.inRange(hsv, np.array(lo), np.array(hi))
        n = cv2.countNonZero(mascara)
        if n > maior:
            melhor, maior = nome, n
    return melhor if maior > 0.02 * hsv.shape[0] * hsv.shape[1] else None

# TODO - analisar
def alinhar_ao_modelo(modelo, marcada):
    """Leva a foto marcada para o mesmo "enquadramento" do modelo.
    Usa pontos de referência (ORB) + transformação de similaridade (deslocamento, rotação
    e escala). Devolve (imagem_alinhada, metodo) ou (marcada_redimensionada, None) se falhar."""
    g1, g2 = para_cinza(modelo), para_cinza(marcada)
    orb = cv2.ORB_create(4000)
    k1, d1 = orb.detectAndCompute(g1, None)
    k2, d2 = orb.detectAndCompute(g2, None)
    if d1 is not None and d2 is not None and len(k1) >= 20 and len(k2) >= 20:
        casam = sorted(cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True).match(d2, d1),
                       key=lambda m: m.distance)[:300]
        if len(casam) >= 15:
            origem = np.float32([k2[m.queryIdx].pt for m in casam])
            destino = np.float32([k1[m.trainIdx].pt for m in casam])
            M, inliers = cv2.estimateAffinePartial2D(origem, destino, method=cv2.RANSAC,
                                                     ransacReprojThreshold=3.0)
            if M is not None and inliers is not None and int(inliers.sum()) >= 15:
                alinhada = cv2.warpAffine(marcada, M, (modelo.shape[1], modelo.shape[0]),
                                          flags=cv2.INTER_LINEAR, borderValue=(255, 255, 255))
                return alinhada, "orb"
    return cv2.resize(marcada, (modelo.shape[1], modelo.shape[0])), None