package com.system.simaf.dto;

import java.util.List;
import java.util.Map;

public record DTOLeituraProva(
    Map<String, String> respostasDetectadas,
    List<Integer> alternativasIds,
    List<String> avisos,
    boolean salvo) {}
