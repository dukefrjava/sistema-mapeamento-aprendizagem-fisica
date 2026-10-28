package com.system.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;

import com.system.simaf.dto.DTOEnvioRespostas;
import com.system.simaf.dto.DTOResultadoAluno;
import com.system.simaf.entity.Avaliacao;
import com.system.simaf.repository.AvaliacaoRepository;
import com.system.simaf.service.RespostaService;
import com.system.simaf.service.ResultadoService;

@RestController
@RequestMapping("/avaliacoes/{avaliacaoId}")
public class AplicacaoController {

    private final RespostaService respostaService;
    private final ResultadoService resultadoService;

    public AplicacaoController(RespostaService respostaService, ResultadoService resultadoService) {
        this.respostaService = respostaService;
        this.resultadoService = resultadoService;
    }

    @PostMapping("/respostas")
    @ResponseStatus(HttpStatus.CREATED)
    public void enviar(@PathVariable Integer avaliacaoId, @RequestBody DTOEnvioRespostas dto) {
        respostaService.enviar(avaliacaoId, dto);
    }

    @GetMapping("/alunos/{alunoId}/resultado")
    public DTOResultadoAluno resultado(@PathVariable Integer avaliacaoId, @PathVariable Integer alunoId) {
        return resultadoService.resultadoDoAluno(avaliacaoId, alunoId);
    }
}