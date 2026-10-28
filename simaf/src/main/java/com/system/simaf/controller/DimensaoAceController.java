package com.system.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.system.simaf.entity.DimensaoAce;
import com.system.simaf.repository.DimensaoAceRepository;

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