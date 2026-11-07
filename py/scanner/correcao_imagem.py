# COMPARA FOTO DA PROVA LIMPA (MODELO) COM A MARCADA, POR SUBTRAÇÃO DE IMAGEM
import re

import cv2

from . import config, ocr, preprocessing as pre


def extrair_regioes_opcoes(img_limpa, idioma=None):
    # {'Q1': [{'letra':'a','caixa':(x0,y0,x1,y1)}]} lido por OCR do modelo
    pals = sorted(ocr.palavras(img_limpa, idioma), key=lambda p: p["y"])
    linhas = []
    for p in pals:
        for linha in linhas:
            if abs(linha[0]["y"] - p["y"]) < 20:
                linha.append(p)
                break
        else:
            linhas.append([p])

    questoes, q_atual = {}, None
    for linha in linhas:
        linha.sort(key=lambda p: p["x"])
        texto = " ".join(p["texto"] for p in linha).lower()
        m_q = re.search(r"quest[aãd]o\s*(\d+)", texto)
        if m_q:
            q_atual = f"Q{m_q.group(1)}"
            questoes[q_atual] = []
            continue
        m_o = re.search(r"^\(?([a-e])\)?", texto)
        if q_atual and m_o:
            questoes[q_atual].append({"letra": m_o.group(1), "caixa": (
                min(p["x"] for p in linha), min(p["y"] for p in linha),
                max(p["x"] + p["w"] for p in linha), max(p["y"] + p["h"] for p in linha))})
    return questoes


# TODO - analisar 
def analisar_marcacoes(modelo_bytes, marcada_bytes, idioma=None):
    """Devolve (respostas {'Q1': 'b' | None}, avisos [str])."""
    limpa = pre.carregar_imagem(modelo_bytes)
    marcada = pre.carregar_imagem(marcada_bytes)
    questoes = extrair_regioes_opcoes(limpa, idioma)

    # Alinha a foto marcada ao modelo ANTES de subtrair: sem isso, qualquer deslocamento
    # ou diferença de recorte faz o texto inteiro aparecer como "marca".
    marcada, metodo = pre.alinhar_ao_modelo(limpa, marcada)
    diferenca = cv2.absdiff(pre.para_cinza(limpa), pre.para_cinza(marcada))
    _, mascara = cv2.threshold(diferenca, config.LIMIAR_DIFERENCA, 255, cv2.THRESH_BINARY)

    respostas, avisos = {}, []
    if metodo is None:
        avisos.append("Não foi possível alinhar a foto ao modelo; o resultado pode estar errado")
    for q, opcoes in questoes.items():
        contagens = []
        for op in opcoes:
            x0, y0, x1, y1 = op["caixa"]
            roi = mascara[max(0, y0 - 15):min(mascara.shape[0], y1 + 15),
                          max(0, x0 - 20):min(mascara.shape[1], x1 + 20)]
            contagens.append((cv2.countNonZero(roi), op["letra"]))
        contagens.sort(reverse=True)
        if contagens and contagens[0][0] > config.MIN_PIXELS_MARCA:
            respostas[q] = contagens[0][1]
            if len(contagens) > 1 and contagens[1][0] > 0.5 * contagens[0][0]:
                avisos.append(f"{q}: marcação ambígua entre '{contagens[0][1]}' e '{contagens[1][1]}'")
        else:
            respostas[q] = None
    if not questoes:
        avisos.append("Nenhuma questão encontrada no modelo (confira idioma do OCR e qualidade da foto)")
    return dict(sorted(respostas.items(), key=lambda kv: int(kv[0][1:]))), avisos