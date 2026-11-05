import fitz # PyMuPDF

def extrair_questoes_pdf(caminho_pdf):
    """
    Lê o PDF nativo, extrai blocos de texto verificando se 
    estão em negrito para definir a alternativa correta 
    """
    doc = fitz.open(caminho_pdf)
    questoes = []

    for pagina in doc:
        blocks = pagina.get_text("dict")["blocks"]
        for b in blocks:
            if "lines" in b:
                for line in b["lines"]:
                    for span in line["spans"]:
                        texto = span["text"]
                        flags = span["flags"]
                        # BIT 2 = NEGRITO (BOLD)
                        is_bold = bool(flags & 2)
    return questoes