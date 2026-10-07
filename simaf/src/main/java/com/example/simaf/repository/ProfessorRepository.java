package com.example.simaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.simaf.entity.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Integer> {
}

