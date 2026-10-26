package com.example.simaf.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "categorias_conceituais")
public class CategoriaConceitual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    public CategoriaConceitual() {}

    public Integer getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}