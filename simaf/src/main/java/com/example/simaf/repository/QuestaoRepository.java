package com.example.simaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.simaf.entity.Questao;

public interface QuestaoRepository extends JpaRepository<Questao, Integer> {
}