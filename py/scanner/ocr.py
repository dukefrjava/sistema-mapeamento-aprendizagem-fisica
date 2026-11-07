# INTEGRAÇÃO COM O TESSERACT
import pytesseract
from pytesseract import Output

from . import config, preprocessing as pre

if config.TESSERACT_CMD:
    pytesseract.pytesseract.tesseract_cmd = config.TESSERACT_CMD

def palavras(img, idioma=None):
    # LISTA DE PALAVRAS COM POSIÇÃO:
    # [{'texto','x','y','w','h}]
    d = pytesseract.image_to_data(pre.para_cinza(img), output_type=Output.DICT, lang=idioma or config.IDIOMA)

    return [{"texto": d["text"][i].strip(), "x": d["left"][i], "y": d["top"][i], "w": d["width"][i], "h": d["height"][i]} for i in range(len(d["text"])) if d["text"][i].strip()]
