import io
import re

import numpy as np
import pdfplumber
import pypdfium2 as pdfium

from . import config

def extrair_opcoes(pdf_bytes):
    opcoes, q_atual = [], None
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        for num_pag, pagina in enumerate(pdf.pages):
            for linha in pagina.extract_text_lines():
                texto = linha["text"].strip()
                m_q = re.match(r"^(\d+)\.\s", texto)
                if m_q:
                    q_atual = f"Q{m_q.group(1)}"
                    continue
                m_o = re.match(r"^([a-e])\.\s", texto)
                if q_atual and m_o:
                    chars = linha["chars"][:2]
                    opcoes.append({
                        "questao": q_atual, "letra": m_o.group(1), "pagina": num_pag,
                        "caixa": (min(c["x0"] for c in chars), min(c["top"] for c in chars),
                                  max(c["x1"] for c in chars), max(c["bottom"] for c in chars)),
                    })
    return opcoes

def analisar_marcacoes(pdf_bytes):
    opcoes = extrair_opcoes(pdf_bytes)
    doc = pdfium.PdfDocument(pdf_bytes)
    try: 
        paginas = [np.array(doc[i].render(scale=config.ESCALA_PDF).to_pil().convert("L"))
                   for i in range(len(doc))]
    finally:
        doc.close()

    esc = config.ESCALA_PDF
    por_questao = {}
    for op in opcoes:
        x0, y0, x1, y1 = op["caixa"]
        # ALARGA UM POUCO A CAIXA PARA PEGAR AS MANCHA QUE PASSA DA LETRA
        xa, ya, xb, yb = int((x0 - 4) * esc), int((y0 - 2) * esc), int((x1 + 2) * esc), int((y1 + 2) * esc)
        # roi = REGION OF INTEREST
        roi = paginas[op["pagina"]][max(0, ya):yb, max(0, xa):xb]
        densidade = float((roi < 128).mean()) if roi.size else 0.0
        por_questao.setdefault(op["questao"], []).append((op["letra"], densidade))

    respostas, avisos = {}, []
    for q, lista in por_questao.items():
        marcadas = [(l, d) for l, d in lista if d >= config.LIMIAR_MARCA]
        if not marcadas:
            respostas[q] = None
        else:
            respostas[q] = max(marcadas, key=lambda t: t[1])[0]
            if len(marcadas) > 1:
                avisos.append(f"{q}: mais de uma marcação ({','.join(l for l, _ in marcadas)})")
    return dict(sorted(respostas.items(), key=lambda kv: int(kv[0][1:]))), avisos
