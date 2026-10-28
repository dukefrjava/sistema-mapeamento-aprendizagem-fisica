package com.system.simaf.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.system.simaf.entity.Resposta;

public interface RespostaRepository extends JpaRepository<Resposta, Integer> {
    List<Resposta> findByAvaliacaoId(Integer avaliacaoId);
    List<Resposta> findByAvaliacaoIdAndAlunoId(Integer avaliacaoId, Integer alunoId);

}