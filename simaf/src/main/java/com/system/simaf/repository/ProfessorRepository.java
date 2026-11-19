package com.system.simaf.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.system.simaf.entity.Professor;
import java.util.List;
import java.util.Optional;


public interface ProfessorRepository extends JpaRepository<Professor, Integer> {

    boolean existsByEmailIgnoreCase(String email);
    Optional<Professor> findFirstyByEmailIgnoreCaseAndSenhaHashIsNotNullOrderByIdAsc(String email);

}

