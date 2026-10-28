package com.system.simaf.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String titulo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private String fase;

    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma; 

    @ManyToOne
    @JoinColumn(name = "professor_id")
    private Professor professor;

    @JsonIgnore
    @OneToMany(mappedBy = "avaliacao")
    private List<Resposta> respostas = new ArrayList<>();
    public List<Resposta> getRespostas() { return respostas; }

    @ManyToMany
    @JoinTable(
        name = "avaliacao_questao",
        joinColumns = @JoinColumn(name = "avaliacao_id"),
        inverseJoinColumns = @JoinColumn(name = "questao_id"))
    private Set<Questao> questoes = new HashSet<>();

    public Avaliacao() {}

    public Integer getId() { return id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }

    public Professor getProfessor() { return professor; }
    public void setProfessor(Professor professor) { this.professor = professor; }

    public Set<Questao> getQuestoes() { return questoes; }
    public void setQuestoes(Set<Questao> questoes) { this.questoes = questoes; }
}