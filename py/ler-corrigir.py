## LÊ PDF

import sys
import json
import re
import cv2
import numpy as np
import pdfplumber
import pypdfium2 as pdfium

ESCALA = 3            # 3x ~ 216 DPI; suficiente para medir a tinta sobre a letra
LIMIAR_MARCA = 0.35   # fração de pixels escuros sobre a letra p/ considerar "marcada"
                      # (letra sem marca ~0.06-0.18 | letra coberta pela mancha ~0.46-0.67)


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


def extrair_opcoes(caminho_pdf):
    """
    Lê a camada de texto do PDF (sem OCR) e devolve, na ordem do documento:
      [{'questao': 'Q1', 'letra': 'a', 'pagina': 0, 'caixa': (x0, y0, x1, y1)}, ...]
    A caixa cobre só o "a." / "b." etc. (em pontos do PDF).
    Questões que atravessam a quebra de página continuam sendo a mesma questão.
    """
    opcoes = []
    q_atual = None

    with pdfplumber.open(caminho_pdf) as pdf:
        for num_pag, pagina in enumerate(pdf.pages):
            for linha in pagina.extract_text_lines():
                texto = linha['text'].strip()

                # Início de questão: "12. Mel is boiling..."
                m_q = re.match(r'^(\d+)\.\s', texto)
                if m_q:
                    q_atual = f"Q{m_q.group(1)}"
                    continue

                # Início de alternativa: "a. ...", "b. ..."
                m_o = re.match(r'^([a-e])\.\s', texto)
                if q_atual and m_o:
                    chars = linha['chars'][:2]  # a letra e o ponto
                    opcoes.append({
                        'questao': q_atual,
                        'letra': m_o.group(1),
                        'pagina': num_pag,
                        'caixa': (min(c['x0'] for c in chars), min(c['top'] for c in chars),
                                  max(c['x1'] for c in chars), max(c['bottom'] for c in chars)),
                    })
    return opcoes


def analisar_marcacoes(caminho_pdf, salvar_debug=True):
    opcoes = extrair_opcoes(caminho_pdf)

    # Rasteriza todas as páginas em tons de cinza
    doc = pdfium.PdfDocument(caminho_pdf)
    paginas = [np.array(doc[i].render(scale=ESCALA).to_pil().convert('L'))
               for i in range(len(doc))]
    debug = [cv2.cvtColor(p, cv2.COLOR_GRAY2BGR) for p in paginas] if salvar_debug else None

    # Mede a "tinta" sobre cada letra
    por_questao = {}
    for op in opcoes:
        x0, y0, x1, y1 = op['caixa']
        # pequena margem para pegar a mancha que extrapola a letra
        xa, ya = int((x0 - 4) * ESCALA), int((y0 - 2) * ESCALA)
        xb, yb = int((x1 + 2) * ESCALA), int((y1 + 2) * ESCALA)
        img = paginas[op['pagina']]
        roi = img[max(0, ya):yb, max(0, xa):xb]
        densidade = float((roi < 128).mean()) if roi.size else 0.0

        por_questao.setdefault(op['questao'], []).append((op['letra'], densidade))

        if salvar_debug:
            cor = (0, 0, 255) if densidade >= LIMIAR_MARCA else (255, 0, 0)
            cv2.rectangle(debug[op['pagina']], (xa, ya), (xb, yb), cor, 2)
            cv2.putText(debug[op['pagina']], f"{densidade:.2f}", (xb + 4, yb),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, cor, 1)

    respostas = {}
    avisos = []
    for q, lista in por_questao.items():
        marcadas = [(l, d) for l, d in lista if d >= LIMIAR_MARCA]
        if not marcadas:
            respostas[q] = None
        else:
            respostas[q] = max(marcadas, key=lambda t: t[1])[0]
            if len(marcadas) > 1:
                avisos.append(f"{q}: mais de uma marcação ({', '.join(l for l, _ in marcadas)})")

    if salvar_debug:
        for i, d in enumerate(debug):
            cv2.imwrite(f"debug_pagina_{i + 1}.png", d)

    return respostas, avisos


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("Uso: python corretor_pdf.py prova_marcada.pdf [gabarito.json]")
        sys.exit(1)

    caminho_pdf = sys.argv[1]
    res, avisos = analisar_marcacoes(caminho_pdf)

    # ordena Q1, Q2, ..., Q10 numericamente
    res = dict(sorted(res.items(), key=lambda kv: int(kv[0][1:])))

    print("\n=============================================")
    print("        RESPOSTAS MARCADAS DETECTADAS        ")
    print("=============================================")
    if not res:
        print("  [Nenhuma questão foi encontrada no PDF]")
    else:
        for q, r in res.items():
            print(f"  {q}: {r.upper() if r else '(Nenhuma marcação detectada)'}")
    for a in avisos:
        print(f"  ⚠️  {a}")

    with open("respostas_detectadas.json", "w", encoding="utf-8") as f:
        json.dump(res, f, ensure_ascii=False, indent=2)

    if len(sys.argv) >= 3:
        with open(sys.argv[2], encoding='utf-8') as f:
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