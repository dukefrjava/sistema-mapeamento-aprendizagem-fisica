package com.example.simaf.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "dimensoes_ace")
public class DimensaoAce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String tipoRaciocinio;

    @Column(nullable = false)
    private String complexidade;

    public DimensaoAce() {}

    public Integer getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTipoRaciocinio() { return tipoRaciocinio; }
    public void setTipoRaciocinio(String tipoRaciocinio) { this.tipoRaciocinio = tipoRaciocinio; }

    public String getComplexidade() { return complexidade; }
    public void setComplexidade(String complexidade) { this.complexidade = complexidade; }
}