package com.system.simaf.controller;

import org.springframework.web.bind.annotation.*;

import com.system.simaf.entity.Resposta;
import com.system.simaf.repository.RespostaRepository;

import java.util.List;

@RestController
@RequestMapping("/respostas")
public class RespostaController {

    private final RespostaRepository repository;

    public RespostaController(RespostaRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Resposta criar(@RequestBody Resposta resposta) {
        return repository.save(resposta);
    }
}