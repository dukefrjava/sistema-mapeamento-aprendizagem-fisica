# EXTRAÇÃO DE TERMOS DE FÍSICA POR REGRAS (REGX + DICIONARIO)
# SEM spaCy

import re
import unicodedata

UNIDADES = r"(?:m/s²|m/s2|m/s|km/h|kg|mg|g|N|J|W|Pa|Hz|K|V|A|Ω|ohms?|mol|cm|mm|km|m|s|L|°\s?[CF])"
RE_UNIDADE = re.compile(rf"(?<![\w.])(-?\d+(?:[.,]\d+)?)\s*({UNIDADES})(?![\w/²])")
RE_FORMULA = re.compile(r"\b[A-Za-zΔ][A-Za-z0-9_]{0,3}\s*=\s*[^\n;]{1,40}")

CONCEITOS = {
    "velocidade", "aceleracao", "forca", "energia", "trabalho", "potencia", "massa", "peso",
    "temperatura", "calor", "pressao", "densidade", "gravidade", "inercia", "momento",
    "corrente", "tensao", "resistencia", "campo", "onda", "frequencia", "refracao",
    "reflexao", "atrito", "equilibrio"
}

def _normalizar(s):
    return unicodedata.normalize("NFKD", s).encode("ascii", "ignore").decode().lower()

def extrair_fisica(texto):
    palavras = set(re.findall(r"[a-z]+", _normalizar(texto)))
    return {
        "unidades": [f"{n} {u}" for n, u in RE_UNIDADE.findall(texto)],
        "formulas": [f.strip() for f in RE_FORMULA.findall(texto)],
        "conceitos": sorted(palavras & CONCEITOS), "unidades": [f"{n} {u}" for n, u in RE_UNIDADE.findall(texto)],
        "formulas": [f.strip() for f in RE_FORMULA.findall(texto)],
        "conceitos": sorted(palavras & CONCEITOS),
    }