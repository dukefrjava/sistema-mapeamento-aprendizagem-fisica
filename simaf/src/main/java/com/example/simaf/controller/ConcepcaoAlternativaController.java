package com.example.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.simaf.entity.ConcepcaoAlternativa;
import com.example.simaf.repository.ConcepcaoAlternativaRepository;

@RestController
@RequestMapping("/concepcoes")
public class ConcepcaoAlternativaController {

    private final ConcepcaoAlternativaRepository repository;

    public ConcepcaoAlternativaController(ConcepcaoAlternativaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ConcepcaoAlternativa> listar() {
        return repository.findAll();
    }

    @PostMapping
    public ConcepcaoAlternativa criar(@RequestBody ConcepcaoAlternativa concepcao) {
        return repository.save(concepcao);
    }
}