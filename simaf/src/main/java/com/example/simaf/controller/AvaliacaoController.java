package com.example.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.simaf.entity.Avaliacao;
import com.example.simaf.repository.AvaliacaoRepository;

@RestController
@RequestMapping("/avaliacoes")
public class AvaliacaoController {

    private final AvaliacaoRepository repository;

    public AvaliacaoController(AvaliacaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Avaliacao> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Avaliacao criar(@RequestBody Avaliacao avaliacao) {
        return repository.save(avaliacao);
    }
}