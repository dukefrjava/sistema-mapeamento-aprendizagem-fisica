from flask import Blueprint, request, jsonify
from scanner.ocr import extrair_questoes_pdf
from scanner.proprecessing import detectar_cor_predominante

@scanner_bp.route('/analisar-prova', methods=['POST'])
def analisar_prova():
    if 'arquivo' not in files:
        return jsonify({"error": "Nenhum arquivo enviado"}), 400

    file = request.files['arquivo']

    return jsonify({"status": "sucesso", "questoes": []})