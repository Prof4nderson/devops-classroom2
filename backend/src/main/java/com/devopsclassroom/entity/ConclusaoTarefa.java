package com.devopsclassroom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Marca de conclusão de uma tarefa (item do checklist) de uma atividade em grupo.
 * A tarefa é identificada pelo índice dentro do checklist do trabalho.
 */
@Entity
@Table(name = "conclusoes_tarefa",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trabalho_id", "equipe_id", "indice_tarefa"}))
public class ConclusaoTarefa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabalho_id")
    @JsonIgnore
    private Trabalho trabalho;

    /** Equipe do aluno; nulo quando a atividade ainda não foi sorteada em grupos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    @JsonIgnore
    private Equipe equipe;

    @Column(name = "indice_tarefa", nullable = false)
    private Integer indiceTarefa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    @JsonIgnore
    private Usuario aluno;

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    @PrePersist
    protected void onCreate() {
        concluidoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Trabalho getTrabalho() { return trabalho; }
    public void setTrabalho(Trabalho trabalho) { this.trabalho = trabalho; }
    public Equipe getEquipe() { return equipe; }
    public void setEquipe(Equipe equipe) { this.equipe = equipe; }
    public Integer getIndiceTarefa() { return indiceTarefa; }
    public void setIndiceTarefa(Integer indiceTarefa) { this.indiceTarefa = indiceTarefa; }
    public Usuario getAluno() { return aluno; }
    public void setAluno(Usuario aluno) { this.aluno = aluno; }
    public LocalDateTime getConcluidoEm() { return concluidoEm; }
}
