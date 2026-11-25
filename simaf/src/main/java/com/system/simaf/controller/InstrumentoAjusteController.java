package com.system.simaf.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.system.simaf.entity.ConcepcaoAlternativa;
import com.system.simaf.entity.DimensaoAce;
import com.system.simaf.entity.Questao;
import com.system.simaf.repository.ConcepcaoAlternativaRepository;
import com.system.simaf.repository.DimensaoAceRepository;
import com.system.simaf.repository.QuestaoRepository;

/** Ajustes pontuais em dados ja cadastrados: dimensao ACE da questao e sugestao de intervencao da concepcao. */
@RestController
public class InstrumentoAjusteController {

    private final QuestaoRepository questoes;
    private final DimensaoAceRepository dimensoes;
    private final ConcepcaoAlternativaRepository concepcoes;

    public InstrumentoAjusteController(QuestaoRepository questoes, DimensaoAceRepository dimensoes,
                                       ConcepcaoAlternativaRepository concepcoes) {
        this.questoes = questoes;
        this.dimensoes = dimensoes;
        this.concepcoes = concepcoes;
    }

    /** Corpo: {"id": 3} com o id de uma dimensao existente. */
    @PutMapping("/questoes/{id}/dimensao")
    public Questao definirDimensao(@PathVariable Integer id, @RequestBody Map<String, Integer> corpo) {
        Questao q = questoes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questao nao encontrada"));
        Integer dimId = corpo.get("id");
        DimensaoAce d = dimId == null ? null : dimensoes.findById(dimId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dimensao nao encontrada"));
        q.setDimensao(d);
        return questoes.save(q);
    }

    /** Corpo: {"sugestaoIntervencao": "texto"}. */
    @PutMapping("/concepcoes/{id}/sugestao")
    public ConcepcaoAlternativa definirSugestao(@PathVariable Integer id, @RequestBody Map<String, String> corpo) {
        ConcepcaoAlternativa c = concepcoes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concepcao nao encontrada"));
        c.setSugestaoIntervencao(corpo.get("sugestaoIntervencao"));
        return concepcoes.save(c);
    }
}
