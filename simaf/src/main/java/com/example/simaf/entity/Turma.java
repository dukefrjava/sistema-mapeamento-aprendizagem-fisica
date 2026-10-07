package com.example.simaf.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "turmas")
public class Turma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private Integer anoLetivo;

    @Column(nullable = false)
    private String serie;

    @ManyToOne
    @JoinColumn(name = "professor_id")
    private Professor professor;

    public Turma() {}

    public Integer getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Integer getAnoLetivo() { return anoLetivo; }
    public void setAnoLetivo(Integer anoLetivo) { this.anoLetivo = anoLetivo; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public Professor getProfessor() { return professor; }
    public void setProfessor(Professor professor) { this.professor = professor; }
}