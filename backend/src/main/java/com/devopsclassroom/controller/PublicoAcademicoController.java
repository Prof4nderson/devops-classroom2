package com.devopsclassroom.controller;

import com.devopsclassroom.repository.CursoRepository;
import com.devopsclassroom.repository.InstituicaoRepository;
import com.devopsclassroom.repository.TurmaRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Dados acadêmicos públicos usados na tela de cadastro do aluno.
 * Expõe somente id/nome/código — nenhum dado pessoal.
 */
@RestController
@RequestMapping("/api/publico/academico")
@Transactional
public class PublicoAcademicoController {

    private final InstituicaoRepository instituicoes;
    private final CursoRepository cursos;
    private final TurmaRepository turmas;

    public PublicoAcademicoController(InstituicaoRepository instituicoes, CursoRepository cursos, TurmaRepository turmas) {
        this.instituicoes = instituicoes;
        this.cursos = cursos;
        this.turmas = turmas;
    }

    @GetMapping("/instituicoes")
    public ResponseEntity<List<Map<String, Object>>> listarInstituicoes() {
        return ResponseEntity.ok(instituicoes.findAll().stream()
                .map(i -> Map.<String, Object>of("id", i.getId(), "nome", i.getNome(),
                        "codigo", i.getCodigo() != null ? i.getCodigo() : ""))
                .toList());
    }

    @GetMapping("/cursos")
    public ResponseEntity<List<Map<String, Object>>> listarCursos(@RequestParam(required = false) Long instituicaoId) {
        return ResponseEntity.ok(cursos.findAll().stream()
                .filter(c -> instituicaoId == null
                        || (c.getInstituicao() != null && instituicaoId.equals(c.getInstituicao().getId())))
                .map(c -> Map.<String, Object>of("id", c.getId(), "nome", c.getNome(),
                        "codigo", c.getCodigo() != null ? c.getCodigo() : "",
                        "instituicaoId", c.getInstituicao() != null ? c.getInstituicao().getId() : 0L))
                .toList());
    }

    @GetMapping("/turmas")
    public ResponseEntity<List<Map<String, Object>>> listarTurmas(@RequestParam(required = false) Long cursoId) {
        var lista = cursoId != null ? turmas.findByCursoId(cursoId) : turmas.findAll();
        return ResponseEntity.ok(lista.stream()
                .map(t -> Map.<String, Object>of("id", t.getId(),
                        "nome", t.getNome() != null ? t.getNome() : t.getCodigo(),
                        "codigo", t.getCodigo(),
                        "cursoId", t.getCurso() != null ? t.getCurso().getId() : 0L,
                        "periodo", t.getPeriodo() != null ? t.getPeriodo().getNome() : ""))
                .toList());
    }
}
