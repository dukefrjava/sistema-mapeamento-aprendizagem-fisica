package com.example.simaf.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "concepcoes_alternativas")
public class ConcepcaoAlternativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(columnDefinition = "TEXT")
    private String sugestaoIntervencao;

    @ManyToMany
    @JoinTable(
        name = "concepcao_categoria",
        joinColumns = @JoinColumn(name = "concepcao_id"),
        inverseJoinColumns = @JoinColumn(name = "categoria_id"))
    private Set<CategoriaConceitual> categorias = new HashSet<>();

    public ConcepcaoAlternativa() {}

    public Integer getId() { return id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getSugestaoIntervencao() { return sugestaoIntervencao; }
    public void setSugestaoIntervencao(String s) { this.sugestaoIntervencao = s; }

    public Set<CategoriaConceitual> getCategorias() { return categorias; }
    public void setCategorias(Set<CategoriaConceitual> categorias) { this.categorias = categorias; }
}