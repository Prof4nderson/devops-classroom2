package com.devopsclassroom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "aulas")
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    @JsonIgnore
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turma_id")
    @JsonIgnore
    private Turma turma;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "data_aula")
    private LocalDateTime dataAula;

    @Column(length = 10)
    private String duracao; // e.g., "2h00"

    @Column(name = "status_aula")
    @Enumerated(EnumType.STRING)
    private StatusAula status;

    /** Momento em que o professor iniciou a aula (abre o diário). */
    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;

    /** Momento em que o professor encerrou a aula (fecha o diário). */
    @Column(name = "finalizada_em")
    private LocalDateTime finalizadaEm;

    /** Conteúdo efetivamente ministrado — registrado no diário. */
    @Column(name = "conteudo_ministrado", length = 4000)
    private String conteudoMinistrado;

    @Column(length = 2000)
    private String observacoes;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
        if (status == null) status = StatusAula.AGENDADA;
    }

    @JsonProperty("cursoId")
    public Long cursoIdJson() { return curso != null ? curso.getId() : null; }

    @JsonProperty("turmaId")
    public Long turmaIdJson() { return turma != null ? turma.getId() : null; }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getDataAula() { return dataAula; }
    public void setDataAula(LocalDateTime dataAula) { this.dataAula = dataAula; }
    public String getDuracao() { return duracao; }
    public void setDuracao(String duracao) { this.duracao = duracao; }
    public StatusAula getStatus() { return status; }
    public void setStatus(StatusAula status) { this.status = status; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(LocalDateTime iniciadaEm) { this.iniciadaEm = iniciadaEm; }
    public LocalDateTime getFinalizadaEm() { return finalizadaEm; }
    public void setFinalizadaEm(LocalDateTime finalizadaEm) { this.finalizadaEm = finalizadaEm; }
    public String getConteudoMinistrado() { return conteudoMinistrado; }
    public void setConteudoMinistrado(String conteudoMinistrado) { this.conteudoMinistrado = conteudoMinistrado; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
