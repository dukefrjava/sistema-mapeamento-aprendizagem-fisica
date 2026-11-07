# LEITURA PDF

import os

# IDIOMA E CONFIG 
IDIOMA = os.environ.get("TESSARACT_LANG", "por+eng")
TESSERACT_CMD = os.environ.get("TESSERACT_CMD")

# renderiza a 3x (~216 DPI): nítido o bastante para medir tinta
ESCALA_PDF = 3

# fração de pixels escuros a partir da qual a letra conta como "marcada"
LIMIAR_MARCA = 0.35

# quanto um pixel precisa mudar (0-255) para contar como "marca"
LIMIAR_DIFERENCA = 30

# quantos pixels mudados uma letra precisa ter para valer como marcada
MIN_PIXELS_MARCA = 30   