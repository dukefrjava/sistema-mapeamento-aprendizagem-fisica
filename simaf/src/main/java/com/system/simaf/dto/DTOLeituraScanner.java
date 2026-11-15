package com.system.simaf.dto;

import java.util.List;
import java.util.Map;

public record DTOLeituraScanner(Map<String, String> respostas, List<String> avisos) { }
