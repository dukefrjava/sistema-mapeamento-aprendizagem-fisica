def calcular_nota(resposta_aluno, gabarito):
    acertos = 0
    detalhe = {}
    for questao, resposta_correta in gabarito.items():
        aluno = resposta_aluno.get(questao)
        aluno = str(aluno).lower() if aluno else None
        correta = str(resposta_correta).lower()
        certo = aluno == correta
        acerto += int(certo)
        detalhe[questao] = {"resposta_aluno": aluno, "resposta_correta": correta, "correto": certo}

    return (acerto / len(gabarito) if gabarito else 0.0), detalhe