package com.system.simaf.dto;

import java.util.List;
import java.util.Map;

public record DTOResultadoAluno(
        Integer alunoId,
        int acertos,
        int total,
        double percentual,
        String desempenho,
        List<String> concepcoesIdentificadas,
        Map<String, Double> acertoPorDimensao) {}