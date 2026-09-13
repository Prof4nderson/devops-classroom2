package com.devopsclassroom.controller;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.AvisoRepository;
import com.devopsclassroom.repository.MatriculaRepository;
import com.devopsclassroom.repository.TurmaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Quadro de avisos do dashboard. Leitura liberada a qualquer usuário autenticado;
 * criação, edição e exclusão restritas a PROFESSOR/ADMIN.
 */
@RestController
@RequestMapping("/api/avisos")
public class AvisoController {

    private static final Set<String> PRIORIDADES = Set.of("INFORMATIVO", "IMPORTANTE", "URGENTE");

    private final AvisoRepository avisos;
    private final TurmaRepository turmas;
    private final MatriculaRepository matriculas;

    public AvisoController(AvisoRepository avisos, TurmaRepository turmas, MatriculaRepository matriculas) {
        this.avisos = avisos;
        this.turmas = turmas;
        this.matriculas = matriculas;
    }

    private Usuario autenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Usuario user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }
        return user;
    }

    private Usuario professor(Authentication auth) {
        Usuario user = autenticado(auth);
        if (user.getTipo() != TipoUsuario.PROFESSOR && user.getTipo() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o professor pode alterar o quadro de avisos");
        }
        return user;
    }

    private Map<String, Object> mapear(Aviso a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("titulo", a.getTitulo());
        m.put("conteudo", a.getConteudo());
        m.put("prioridade", a.getPrioridade());
        m.put("fixado", a.isFixado());
        m.put("validoAte", a.getValidoAte() != null ? a.getValidoAte().toString() : null);
        m.put("turmaId", a.getTurma() != null ? a.getTurma().getId() : null);
        m.put("turmaNome", a.getTurma() != null
                ? (a.getTurma().getNome() != null ? a.getTurma().getNome() : a.getTurma().getCodigo()) : null);
        m.put("autorNome", a.getAutor() != null ? a.getAutor().getNome() : null);
        m.put("criadoEm", a.getCriadoEm() != null ? a.getCriadoEm().toString() : null);
        return m;
    }

    private void aplicar(Aviso a, Map<String, Object> body) {
        if (body.get("titulo") != null) a.setTitulo(body.get("titulo").toString().trim());
        if (body.get("conteudo") != null) a.setConteudo(body.get("conteudo").toString().trim());
        if (body.get("prioridade") != null) {
            String p = body.get("prioridade").toString().toUpperCase(Locale.ROOT);
            a.setPrioridade(PRIORIDADES.contains(p) ? p : "INFORMATIVO");
        }
        if (body.get("fixado") != null) a.setFixado(Boolean.parseBoolean(body.get("fixado").toString()));
        if (body.containsKey("validoAte")) {
            Object v = body.get("validoAte");
            if (v == null || v.toString().isBlank()) {
                a.setValidoAte(null);
            } else {
                String texto = v.toString();
                a.setValidoAte(LocalDateTime.parse(texto.length() == 16 ? texto + ":00" : texto));
            }
        }
        if (body.containsKey("turmaId")) {
            Object t = body.get("turmaId");
            if (t == null || t.toString().isBlank()) {
                a.setTurma(null);
            } else {
                a.setTurma(turmas.findById(Long.valueOf(t.toString()))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada")));
            }
        }
        if (a.getTitulo() == null || a.getTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o título do aviso");
        }
        if (a.getConteudo() == null || a.getConteudo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o texto do aviso");
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(Authentication auth) {
        Usuario user = autenticado(auth);
        LocalDateTime agora = LocalDateTime.now();

        Set<Long> minhasTurmas = new HashSet<>();
        if (user.getTipo() == TipoUsuario.ALUNO) {
            matriculas.findByUsuarioId(user.getId()).forEach(m -> {
                if (m.getTurma() != null) minhasTurmas.add(m.getTurma().getId());
            });
        }

        List<Map<String, Object>> lista = avisos.findAllByOrderByFixadoDescCriadoEmDesc().stream()
                .filter(a -> a.getValidoAte() == null || a.getValidoAte().isAfter(agora))
                .filter(a -> user.getTipo() != TipoUsuario.ALUNO
                        || a.getTurma() == null
                        || minhasTurmas.contains(a.getTurma().getId()))
                .map(this::mapear)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> criar(@RequestBody Map<String, Object> body, Authentication auth) {
        Usuario prof = professor(auth);
        Aviso a = new Aviso();
        a.setAutor(prof);
        aplicar(a, body);
        return ResponseEntity.ok(mapear(avisos.save(a)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                         Authentication auth) {
        professor(auth);
        Aviso a = avisos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso não encontrado"));
        aplicar(a, body);
        return ResponseEntity.ok(mapear(avisos.save(a)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication auth) {
        professor(auth);
        avisos.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
