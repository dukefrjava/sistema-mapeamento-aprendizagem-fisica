package com.example.simaf.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.simaf.entity.CategoriaConceitual;
import com.example.simaf.repository.CategoriaConceitualRepository;

@RestController
@RequestMapping("/categorias")
public class CategoriaConceitualController {

    private final CategoriaConceitualRepository repository;

    public CategoriaConceitualController(CategoriaConceitualRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CategoriaConceitual> listar() {
        return repository.findAll();
    }

    @PostMapping
    public CategoriaConceitual criar(@RequestBody CategoriaConceitual categoria) {
        return repository.save(categoria);
    }
}