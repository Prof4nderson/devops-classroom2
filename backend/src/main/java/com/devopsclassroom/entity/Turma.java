package com.devopsclassroom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "turmas", uniqueConstraints = @UniqueConstraint(columnNames = {"curso_id", "periodo_id", "codigo"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Turma {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String codigo;
    @Column(length = 160) private String nome;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "curso_id") @JsonIgnore private Curso curso;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "periodo_id") @JsonIgnore private Periodo periodo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "professor_id") @JsonIgnore private Usuario professor;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
    public Periodo getPeriodo() { return periodo; }
    public void setPeriodo(Periodo periodo) { this.periodo = periodo; }
    public Usuario getProfessor() { return professor; }
    public void setProfessor(Usuario professor) { this.professor = professor; }
}
