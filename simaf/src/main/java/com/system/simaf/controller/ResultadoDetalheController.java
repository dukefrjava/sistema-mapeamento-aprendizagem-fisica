package com.system.simaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.simaf.dto.DTOResultadoDetalhe;
import com.system.simaf.service.ResultadoDetalheService;

@RestController
@RequestMapping("/avaliacoes/{avaliacaoId}/alunos/{alunoId}")
public class ResultadoDetalheController {

    private final ResultadoDetalheService service;

    public ResultadoDetalheController(ResultadoDetalheService service) {
        this.service = service;
    }

    /** Resultado individual completo (questão a questão, dimensões, concepções, comparação com a turma). */
    @GetMapping("/detalhe")
    public DTOResultadoDetalhe detalhe(@PathVariable Integer avaliacaoId, @PathVariable Integer alunoId) {
        return service.detalhe(avaliacaoId, alunoId);
    }
}
