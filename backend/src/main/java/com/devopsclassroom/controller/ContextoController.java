package com.devopsclassroom.controller;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contexto acadêmico do usuário logado (instituição, curso, turma e período),
 * usado no cabeçalho do dashboard.
 */
@RestController
@RequestMapping("/api/contexto")
public class ContextoController {

    private final MatriculaRepository matriculas;
    private final TurmaRepository turmas;

    public ContextoController(MatriculaRepository matriculas, TurmaRepository turmas) {
        this.matriculas = matriculas;
        this.turmas = turmas;
    }

    @GetMapping("/meu")
    @Transactional
    public Map<String, Object> meuContexto(Authentication auth) {
        Map<String, Object> ctx = new HashMap<>();
        if (auth == null || !(auth.getPrincipal() instanceof Usuario user)) {
            return ctx;
        }

        ctx.put("nome", user.getNome());
        ctx.put("tipo", user.getTipo() != null ? user.getTipo().name() : null);
        ctx.put("instituicao", user.getInstituicao());

        if (user.getTipo() == TipoUsuario.PROFESSOR || user.getTipo() == TipoUsuario.ADMIN) {
            List<Turma> minhas = turmas.findAll().stream()
                    .filter(t -> t.getProfessor() != null && t.getProfessor().getId() != null
                            && t.getProfessor().getId().equals(user.getId()))
                    .toList();
            if (!minhas.isEmpty()) {
                Turma turma = minhas.get(0);
                preencher(ctx, turma);
                ctx.put("totalTurmas", minhas.size());
            }
            return ctx;
        }

        List<Matricula> lista = matriculas.findByUsuarioId(user.getId());
        Matricula ativa = lista.stream()
                .filter(m -> m.getStatus() == null || m.getStatus() == StatusMatricula.ATIVA)
                .findFirst()
                .orElse(lista.isEmpty() ? null : lista.get(0));

        if (ativa != null) {
            if (ativa.getCurso() != null) {
                ctx.put("curso", ativa.getCurso().getNome());
                ctx.put("cursoCodigo", ativa.getCurso().getCodigo());
            }
            preencher(ctx, ativa.getTurma());
        }
        return ctx;
    }

    private void preencher(Map<String, Object> ctx, Turma turma) {
        if (turma == null) return;
        ctx.put("turma", turma.getNome() != null ? turma.getNome() : turma.getCodigo());
        ctx.put("turmaCodigo", turma.getCodigo());
        try {
            if (turma.getCurso() != null) {
                ctx.putIfAbsent("curso", turma.getCurso().getNome());
                ctx.putIfAbsent("cursoCodigo", turma.getCurso().getCodigo());
            }
            if (turma.getPeriodo() != null) {
                ctx.put("periodo", turma.getPeriodo().getNome());
                if (turma.getPeriodo().getInstituicao() != null) {
                    ctx.putIfAbsent("instituicao", turma.getPeriodo().getInstituicao().getNome());
                }
            }
        } catch (Exception ignored) {
            // relações lazy indisponíveis: mantém o que já foi preenchido
        }
    }
}
