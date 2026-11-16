package com.system.simaf.dto;

import java.util.List;
import java.util.Map;

// RESUMO DE UMA AVALIAÇÃO PARA O PROFESSOR
public record DTODashboard(
    Integer avaliacaoId,
    String titulo,
    int alunoResponderam,
    double mediaPercentual,
    Map<String, Integer> distribuicao,
    List<ItemQuestao> questoes,
    List<ItemDificuldade> categorias,
    List<ItemConcepcao> concepcoes,
    Map<String, Double> acertoPorDimensao,
    List<ItemAluno> alunos) {

        public record ItemQuestao(Integer numero, String enunciado, int respondentes, int acertos, double percentualAcerto) {}
    
        public record ItemDificuldade(String codigo, String nome, int ocorrencias) {}

        public record ItemConcepcao(String descricao, String sugestaoIntervencao, int ocorrencias) {}

        public record ItemAluno(Integer alunoId, String matricula, int acertos, int total, double percentual, String desempenho) {}
    }