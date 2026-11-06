from flask import Blueprint, jsonify, request

from . import correcao_pdf

bp = Blueprint("scanner",__name__)

ASSINATURA_PDF = b"%PDF-"

class ErroEntrada(Exception):
    def __init__(self, msg, status=400):
        super().__init__(msg)
        self.status = status

def _arquivo_pdf(campo):
    f = request.files.get(campo)
    if f is None: 
        raise ErroEntrada(f"Campo '{campo}' ausente")
    dados = f.read()
    if not dados.startswith(ASSINATURA_PDF):
        raise ErroEntrada(f"'{campo}' não é um PDF válido", 415)
    return dados

@bp.errorhandler(ErroEntrada)
def _entrada(e):
    return jsonify(erro=str(e)), e.status

@bp.get("/health")
def health():
    return jsonify(status="ok")

@bp.post("/anlisar-pdf")
def analisar_pdf():
    respostas, avisos = correcao_pdf.analisar_marcacoes(_arquivo_pdf("arquivo"))
    return jsonify(respostas=respostas, avisos=avisos)
