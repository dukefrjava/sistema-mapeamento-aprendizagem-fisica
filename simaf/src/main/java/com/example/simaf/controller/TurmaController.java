package com.example.simaf.controller;

import org.springframework.web.bind.annotation.*;

import com.example.simaf.entity.Turma;
import com.example.simaf.repository.TurmaRepository;

import java.util.List;

@RestController
@RequestMapping("/turmas")
public class TurmaController {

    private final TurmaRepository repository;

    public TurmaController(TurmaRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Turma> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Turma criar(@RequestBody Turma turma) {
        return repository.save(turma);
    }

}