package com.example.simaf.entity;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "alternativas")
public class Alternativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String letra;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private boolean correta;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "questao_id", nullable = false)
    private Questao questao;

    @ManyToMany
    @JoinTable(
        name = "alternativa_concepcao",
        joinColumns = @JoinColumn(name = "alternativa_id"),
        inverseJoinColumns = @JoinColumn(name = "concepcao_id"))
    private Set<ConcepcaoAlternativa> concepcoes = new HashSet<>();

    public Alternativa() {}

    public Integer getId() { return id; }

    public String getLetra() { return letra; }
    public void setLetra(String letra) { this.letra = letra; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public boolean isCorreta() { return correta; }
    public void setCorreta(boolean correta) { this.correta = correta; }

    public Questao getQuestao() { return questao; }
    public void setQuestao(Questao questao) { this.questao = questao; }

    public Set<ConcepcaoAlternativa> getConcepcoes() { return concepcoes; }
    public void setConcepcoes(Set<ConcepcaoAlternativa> concepcoes) { this.concepcoes = concepcoes; }
}