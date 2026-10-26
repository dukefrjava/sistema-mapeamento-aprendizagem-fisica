package com.example.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.simaf.entity.DimensaoAce;
import com.example.simaf.repository.DimensaoAceRepository;

@RestController
@RequestMapping("/dimensoes")
public class DimensaoAceController {

    private final DimensaoAceRepository repository;

    public DimensaoAceController(DimensaoAceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<DimensaoAce> listar() {
        return repository.findAll();
    }

    @PostMapping
    public DimensaoAce criar(@RequestBody DimensaoAce dimensao) {
        return repository.save(dimensao);
    }
}