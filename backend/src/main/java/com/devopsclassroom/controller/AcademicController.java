package com.devopsclassroom.controller;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/academico")
public class AcademicController {
    private final InstituicaoRepository instituicoes;
    private final PeriodoRepository periodos;
    private final TurmaRepository turmas;
    private final CursoRepository cursos;
    private final UsuarioRepository usuarios;
    public AcademicController(InstituicaoRepository instituicoes, PeriodoRepository periodos, TurmaRepository turmas,
                               CursoRepository cursos, UsuarioRepository usuarios) {
        this.instituicoes = instituicoes; this.periodos = periodos; this.turmas = turmas;
        this.cursos = cursos; this.usuarios = usuarios;
    }
    private void professor(Authentication auth) {
        Usuario user = (Usuario) auth.getPrincipal();
        if (user.getTipo() != TipoUsuario.PROFESSOR && user.getTipo() != TipoUsuario.ADMIN) throw new RuntimeException("Acesso permitido somente ao professor");
    }
    @GetMapping("/instituicoes") public List<Instituicao> listarInstituicoes() { return instituicoes.findAll(); }
    @PostMapping("/instituicoes") public Instituicao criarInstituicao(@RequestBody Instituicao item, Authentication auth) { professor(auth); return instituicoes.save(item); }
    @PutMapping("/instituicoes/{id}") public ResponseEntity<Instituicao> atualizarInstituicao(@PathVariable Long id, @RequestBody Instituicao item, Authentication auth) { professor(auth); return instituicoes.findById(id).map(old -> { old.setNome(item.getNome()); old.setCodigo(item.getCodigo()); old.setDescricao(item.getDescricao()); return ResponseEntity.ok(instituicoes.save(old)); }).orElse(ResponseEntity.notFound().build()); }
    @DeleteMapping("/instituicoes/{id}") public ResponseEntity<Void> excluirInstituicao(@PathVariable Long id, Authentication auth) { professor(auth); if (!instituicoes.existsById(id)) return ResponseEntity.notFound().build(); instituicoes.deleteById(id); return ResponseEntity.noContent().build(); }
    @GetMapping("/instituicoes/{id}/periodos") public List<Periodo> listarPeriodos(@PathVariable Long id) { return periodos.findByInstituicaoId(id); }
    @PostMapping("/instituicoes/{id}/periodos") public Periodo criarPeriodo(@PathVariable Long id, @RequestBody Periodo item, Authentication auth) { professor(auth); item.setInstituicao(instituicoes.findById(id).orElseThrow(() -> new RuntimeException("Instituição não encontrada"))); return periodos.save(item); }
    @GetMapping("/cursos/{cursoId}/turmas") public List<Turma> listarTurmas(@PathVariable Long cursoId) { return turmas.findByCursoId(cursoId); }
    @PostMapping("/cursos/{cursoId}/turmas") public Turma criarTurma(@PathVariable Long cursoId, @RequestBody Map<String,Object> body, Authentication auth) { professor(auth); Turma turma = new Turma(); turma.setCodigo(String.valueOf(body.getOrDefault("codigo", ""))); turma.setNome(String.valueOf(body.getOrDefault("nome", turma.getCodigo()))); turma.setCurso(cursos.findById(cursoId).orElseThrow(() -> new RuntimeException("Curso não encontrado"))); Long periodoId = Long.valueOf(String.valueOf(body.get("periodoId"))); turma.setPeriodo(periodos.findById(periodoId).orElseThrow(() -> new RuntimeException("Período não encontrado"))); Usuario professor = (Usuario) auth.getPrincipal(); turma.setProfessor(professor); return turmas.save(turma); }
    @PutMapping("/turmas/{id}") public ResponseEntity<Turma> atualizarTurma(@PathVariable Long id, @RequestBody Turma item, Authentication auth) { professor(auth); return turmas.findById(id).map(old -> { old.setCodigo(item.getCodigo()); old.setNome(item.getNome()); return ResponseEntity.ok(turmas.save(old)); }).orElse(ResponseEntity.notFound().build()); }
    @DeleteMapping("/turmas/{id}") public ResponseEntity<Void> excluirTurma(@PathVariable Long id, Authentication auth) { professor(auth); if (!turmas.existsById(id)) return ResponseEntity.notFound().build(); turmas.deleteById(id); return ResponseEntity.noContent().build(); }
}
