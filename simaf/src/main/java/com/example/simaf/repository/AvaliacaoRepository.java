package com.example.simaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.simaf.entity.Avaliacao;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Integer> {
}