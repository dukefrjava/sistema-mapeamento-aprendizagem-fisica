package com.example.simaf.controller;

import org.springframework.web.bind.annotation.*;

import com.example.simaf.entity.Resposta;
import com.example.simaf.repository.RespostaRepository;

import java.util.List;

@RestController
@RequestMapping("/respostas")
public class RespostaController {

    private final RespostaRepository repository;

    public RespostaController(RespostaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Resposta> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Resposta criar(@RequestBody Resposta resposta) {
        return repository.save(resposta);
    }
}