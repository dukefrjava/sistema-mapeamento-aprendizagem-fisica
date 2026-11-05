def estruturar_dados_prova(texto_bruto, cor_detectada):
    """
    Converte o texto bruto da prova e a cor detectada no formato
    esperado pelo endpoint /questoes dp Spring Boot
    """

    # MAPEAMENTO DE DIMENSÕES
    dimensao_map = {
        "Apply": 1,
        "Create": 2,
        "Evaluate": 3
    }

    questao_json = {
        "enunciado": "Texto extraído da questão...",
        "instrumento": "Prova Escrita",
        "tipo": "Múltipla Escolha",
        "descartada": False,
        "dimensao": {"id": dimensao_map.get(cor_detectada, 1)},
        "alternativas": [
            { "letra": "A", "texto": "Alternativa errada normal", "correta": False },
            { "letra": "B", "texto": "Alternativa correta em negrito", "correta": True }
        ]
    }
    return questao_json