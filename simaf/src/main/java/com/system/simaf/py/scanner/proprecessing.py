import cv2
import numpy as np

def detectar_cor_predominante(roi):
    """
    Analisa a região de interesse (ROI) e retorna a taxonomia 
    com base na cor predominate:
    - Vermelho -> Apply
    - Verde -> Create
    - Azul -> Evaluate
    """
    hsv = cv2.cvtColor(roi, cv2.COLOR_BGR2HSV)

    # INTERVALOS DE CORES APROXIMADOS EM HSV

    # VERMELHO - PRECISA ABRANGER OS DOIS EXTREMOS DO HUE
    limite_inferior_vermelho = np.array([0, 50, 50]) 
    limite_superior_vermelho = np.array([10, 255, 255])
    mascara_vermelho = cv2.inRange(hsv, limite_inferior_vermelho, limite_superior_vermelho)

    # VERDE
    limite_inferior_verde = np.array([35, 50, 50]) 
    limite_superior_verde = np.array([85, 255, 255])
    mascara_verde = cv2.inRange(hsv, limite_inferior_verde, limite_superior_verde)

    # AZUL
    limite_inferior_azul = np.array([0, 50, 50]) 
    limite_superior_azul = np.array([10, 255, 255])
    mascara_azul = cv2.inRange(hsv, limite_inferior_azul, limite_superior_azul)

    pixel_vermelho = cv2.countNonZero(mascara_vermelho)
    pixel_verde = cv2.countNonZero(mascara_verde)
    pixel_azul = cv2.countNonZero(mascara_azul)

    pixel_max = max(pixel_vermelho, pixel_verde, pixel_azul)
    if pixel_max < 100: return "Geral"

    if pixel_max == pixel_vermelho: return "Apply"
    elif pixel_max == pixel_verde: return "Create"
    else: return "Evaluate"


