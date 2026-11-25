package com.system.simaf.dto;

import java.util.List;

/** Resultado individual completo de um aluno em uma avaliação (tela resultado.html). */
public record DTOResultadoDetalhe(
        AlunoInfo aluno,
        AvaliacaoInfo avaliacao,
        int acertos,
        int erros,
        int semResposta,
        int total,
        double percentual,
        String desempenho,
        double mediaTurma,
        int posicao,
        int totalAlunos,
        List<QuestaoItem> questoes,
        List<DimensaoItem> dimensoes,
        List<CategoriaItem> categorias,
        List<ConcepcaoItem> concepcoes) {

    public record AlunoInfo(Integer id, String matricula, String turma) {}

    public record AvaliacaoInfo(Integer id, String titulo) {}

    public record QuestaoItem(int numero, String enunciado, boolean respondida, boolean acertou,
                              String marcada, String correta, double percentualTurma,
                              List<String> concepcoes) {}

    public record DimensaoItem(String codigo, int acertos, int total, double percentual,
                               double percentualTurma) {}

    public record CategoriaItem(String codigo, String nome, int erros) {}

    public record ConcepcaoItem(String descricao, String sugestaoIntervencao, int ocorrencias,
                                List<String> categorias) {}
}
