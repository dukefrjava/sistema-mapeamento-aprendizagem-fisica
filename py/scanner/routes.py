from flask import Blueprint, jsonify, request
from PIL import UnidentifiedImageError

from . import correcao_imagem, correcao_pdf

bp = Blueprint("scanner",__name__)

ASSINATURA = {"pdf": [b"%PDF-"], "imagem": [b"\x89PNG\r\n\x1a\n", b"\xff\xd8\xff"]}

class ErroEntrada(Exception):
    def __init__(self, msg, status=400):
        super().__init__(msg)
        self.status = status

def _arquivo(campo, tipo):
    f = request.files.get(campo)
    if f is None: 
        raise ErroEntrada(f"Campo '{campo}' ausente")
    dados = f.read()
    if not dados:
        raise ErroEntrada(f"'Arquivo '{campo}' vazio")
    if not any(dados.startswith(a) for a in ASSINATURA[tipo]):
        raise ErroEntrada(f"'{campo}' não é um arquivo {tipo} válido (aceitos: PDF, PNG, JPEG)", 415)
    return dados

@bp.errorhandler(ErroEntrada)
def _entrada(e):
    return jsonify(erro=str(e)), e.status

@bp.errorhandler(UnidentifiedImageError)
@bp.errorhandler(ValueError)
def _invalido(e):
    return jsonify(erro="Arquvo ilegível ou corrrompido"), 422

@bp.get("/health")
def health():
    return jsonify(status="ok")

@bp.post("/analisar-pdf")
def analisar_pdf():
    respostas, avisos = correcao_pdf.analisar_marcacoes(_arquivo("arquivo"))
    return jsonify(respostas=respostas, avisos=avisos)

@bp.post("/analisar-imagem")
def analisar_imagem():
    modelo, marcada = _arquivo("modelo", "imagem"), _arquivo("marcada", "imagem")
    respostas, avisos = correcao_imagem.analisar_marcacoes(modelo, marcada)
    return jsonify(repostas=respostas, avisos=avisos)
