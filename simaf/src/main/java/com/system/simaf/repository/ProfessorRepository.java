package com.system.simaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.system.simaf.entity.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Integer> {
}

