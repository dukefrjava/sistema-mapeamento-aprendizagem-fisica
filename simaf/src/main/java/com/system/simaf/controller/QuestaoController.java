package com.system.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.system.simaf.entity.Questao;
import com.system.simaf.repository.QuestaoRepository;

@RestController
@RequestMapping("/questoes")
public class QuestaoController {

    private final QuestaoRepository repository;

    public QuestaoController(QuestaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Questao> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Questao criar(@RequestBody Questao questao) {
        return repository.save(questao);
    }
}