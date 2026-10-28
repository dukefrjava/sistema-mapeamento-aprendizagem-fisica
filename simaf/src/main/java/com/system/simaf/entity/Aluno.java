package com.system.simaf.entity;

import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "alunos")
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String matricula;

    @Column(nullable = false)
    private String email;

    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @JsonIgnore
    @OneToMany(mappedBy = "aluno")
    private List<Resposta> respostas = new ArrayList<>();
    public List<Resposta> getRespostas() { return respostas; }

    public Aluno() {}

    public Integer getId() { return id; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }
}

