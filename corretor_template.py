import sys
import json
import re
import cv2
import numpy as np
import pytesseract
from pytesseract import Output

pytesseract.pytesseract.tesseract_cmd = r'C:\Program Files\Tesseract-OCR\tesseract.exe'

def calcular_nota(resposta_aluno, gabarito): 
    acertos = 0
    detalhe = {}
    for questao, resposta_correta in gabarito.items():
        resp_aluno_str = str(resposta_aluno.get(questao)).lower() if resposta_aluno.get(questao) else None
        resp_correta_str = str(resposta_correta).lower()

        correto = (resp_aluno_str == resp_correta_str)
        acertos += int(correto)
        detalhe[questao] = {
            'resposta_aluno': resp_aluno_str,
            'resposta_correta': resp_correta_str,
            'correto': correto,
        }
    nota = acertos / len(gabarito) if gabarito else 0.0
    return nota, detalhe

def extrair_regioes_opcoes(caminho_limpa, idioma='por'):
    img_limpa = cv2.imread(caminho_limpa)
    if img_limpa is None:
        raise FileNotFoundError(f"Não foi possível abrir: {caminho_limpa}")

    cinza = cv2.cvtColor(img_limpa, cv2.COLOR_BGR2GRAY)
    dados = pytesseract.image_to_data(cinza, output_type=Output.DICT, lang=idioma)

    palavras = []
    for i in range(len(dados['text'])):
        texto = dados['text'][i].strip()
        if texto:
            palavras.append({
                'texto': texto,
                'x': dados['left'][i], 'y': dados['top'][i],
                'w': dados['width'][i], 'h': dados['height'][i]
            })

    # Agrupa em linhas por proximidade vertical
    palavras.sort(key=lambda p: p['y'])
    linhas = []
    for p in palavras:
        colocado = False
        for linha in linhas:
            if abs(linha[0]['y'] - p['y']) < 20:
                linha.append(p)
                colocado = True
                break
        if not colocado:
            linhas.append([p])

    for l in linhas:
        l.sort(key=lambda p: p['x'])

    questoes = {}
    q_atual = None

    for linha in linhas:
        texto_linha = ' '.join(p['texto'] for p in linha).lower()

        # Procura por padrões tipo "Questão 1" ou "questao 1"
        match_q = re.search(r'quest[aã]o\s*(\d+)', texto_linha)
        if match_q:
            q_atual = f"Q{match_q.group(1)}"
            questoes[q_atual] = []
            continue

        # Extrai a letra diretamente da expressão regular ex: (a), a), (b)...
        match_opt = re.search(r'^\(?([a-e])\)?', texto_linha)
        if q_atual and match_opt:
            letra_detectada = match_opt.group(1) # Extrai a letra real escrita na linha
            
            x0 = min(p['x'] for p in linha)
            y0 = min(p['y'] for p in linha)
            x1 = max(p['x'] + p['w'] for p in linha)
            y1 = max(p['y'] + p['h'] for p in linha)

            questoes[q_atual].append({'letra': letra_detectada, 'caixa': (x0, y0, x1, y1)})

    return img_limpa, questoes
    
def analisar_marcacoes(caminho_limpa, caminho_marcada):
    img_limpa, questoes = extrair_regioes_opcoes(caminho_limpa)
    img_marcada = cv2.imread(caminho_marcada)

    if img_marcada is None:
        raise FileNotFoundError(f"Não foi possível abrir a imagem marcada no caminho: {caminho_marcada}")

    if img_limpa.shape != img_marcada.shape:
        img_marcada = cv2.resize(img_marcada, (img_limpa.shape[1], img_limpa.shape[0]))

    cinza_limpa = cv2.cvtColor(img_limpa, cv2.COLOR_BGR2GRAY)
    cinza_marcada = cv2.cvtColor(img_marcada, cv2.COLOR_BGR2GRAY)

    # Subtração das duas imagens
    diferenca = cv2.absdiff(cinza_limpa, cinza_marcada)
    _, mascara = cv2.threshold(diferenca, 30, 255, cv2.THRESH_BINARY)

    respostas = {}
    debug_img = img_marcada.copy()

    for q, opcoes in questoes.items():
        melhor_letra = None
        max_pixels = 0

        for op in opcoes:
            letra = op['letra']
            x0, y0, x1, y1 = op['caixa']
            
            # Margens expandidas (-15/+15 vertical e -20/+20 horizontal)
            roi = mascara[max(0, y0-15):min(mascara.shape[0], y1+15), 
                          max(0, x0-20):min(mascara.shape[1], x1+20)]
            pixels_marcados = cv2.countNonZero(roi)

            # Desenha as caixas maiores no debug_resultado.png
            cv2.rectangle(debug_img, (max(0, x0-20), max(0, y0-15)), 
                          (min(debug_img.shape[1], x1+20), min(debug_img.shape[0], y1+15)), (255, 0, 0), 1)

            if pixels_marcados > max_pixels and pixels_marcados > 30:
                max_pixels = pixels_marcados
                melhor_letra = letra

        respostas[q] = melhor_letra

    cv2.imwrite("debug_subtracao.png", mascara)
    cv2.imwrite("debug_resultado.png", debug_img)
    return respostas

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Uso: python corretor_template.py img-teste/limpa.png img-teste/marcada.png [gabarito.json]")
        sys.exit(1)

    caminho_limpa = sys.argv[1]
    caminho_marcada = sys.argv[2]

    res = analisar_marcacoes(caminho_limpa, caminho_marcada)

    print("\n=============================================")
    print("        RESPOSTAS MARCADAS DETECTADAS        ")
    print("=============================================")
    if not res:
        print("  [Nenhuma questão foi mapeada do template limpo]")
    else:
        for q, r in res.items():
            print(f"  {q}: {r.upper() if r else '(Nenhuma marcação detectada)'}")

    if len(sys.argv) >= 4:
        caminho_gabarito = sys.argv[3]
        with open(caminho_gabarito, encoding='utf-8') as f:
            gabarito = json.load(f)

        nota, detalhe = calcular_nota(res, gabarito)

        print("\n=============================================")
        print("            RESULTADO DA CORREÇÃO            ")
        print("=============================================")
        for q, d in detalhe.items():
            status = '✅ CORRETO' if d['correto'] else '❌ INCORRETO'
            aluno = d['resposta_aluno'].upper() if d['resposta_aluno'] else 'N'
            correta = d['resposta_correta'].upper()
            print(f"  {q} | Aluno: {aluno} | Gabarito: {correta} | {status}")
        
        print("\n========================================")
        print(f"          NOTA FINAL: {nota:.2%}         ")
        print("========================================\n")