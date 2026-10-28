package com.system.simaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.system.simaf.entity.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Integer> {
}