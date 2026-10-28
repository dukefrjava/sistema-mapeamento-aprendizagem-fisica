package com.system.simaf.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "questoes")
public class Questao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String enunciado;

    @Column(nullable = false)
    private String instrumento;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private boolean descartada;

    @ManyToOne
    @JoinColumn(name = "dimensao_id")
    private DimensaoAce dimensao;

    // Composição: as alternativas nascem e morrem com a questão
    @OneToMany(mappedBy = "questao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Alternativa> alternativas = new ArrayList<>();

    public Questao() {}

    public Integer getId() { return id; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public String getInstrumento() { return instrumento; }
    public void setInstrumento(String instrumento) { this.instrumento = instrumento; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public boolean isDescartada() { return descartada; }
    public void setDescartada(boolean descartada) { this.descartada = descartada; }

    public DimensaoAce getDimensao() { return dimensao; }
    public void setDimensao(DimensaoAce dimensao) { this.dimensao = dimensao; }

    public List<Alternativa> getAlternativas() { return alternativas; }

    // Preenche o lado "dono" da relação ao receber o JSON
    public void setAlternativas(List<Alternativa> alternativas) {
        this.alternativas.clear();
        if (alternativas != null) {
            alternativas.forEach(a -> {
                a.setQuestao(this);
                this.alternativas.add(a);
            });
        }
    }
}