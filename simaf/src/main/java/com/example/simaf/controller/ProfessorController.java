package com.example.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.simaf.entity.Professor;
import com.example.simaf.repository.ProfessorRepository;

@RestController
@RequestMapping("/professores")
public class ProfessorController {

    private final ProfessorRepository repository;

    public ProfessorController(ProfessorRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Professor> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Professor criar(@RequestBody Professor professor) {
        return repository.save(professor);
    }
}