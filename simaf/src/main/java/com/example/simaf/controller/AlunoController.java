package com.example.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.simaf.entity.Aluno;
import com.example.simaf.repository.AlunoRepository;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunoRepository repository;

    public AlunoController(AlunoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Aluno> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Aluno criar(@RequestBody Aluno aluno) {
        return repository.save(aluno);
    }
}