package com.devopsclassroom.controller;

import com.devopsclassroom.dto.CursoRequest;
import com.devopsclassroom.dto.MatriculaRequest;
import com.devopsclassroom.dto.UsuarioResponse;
import com.devopsclassroom.entity.Curso;
import com.devopsclassroom.entity.Usuario;
import com.devopsclassroom.entity.TipoUsuario;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.devopsclassroom.service.CursoService;
import com.devopsclassroom.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoService cursoService;
    private final UsuarioService usuarioService;

    public CursoController(CursoService cursoService, UsuarioService usuarioService) {
        this.cursoService = cursoService;
        this.usuarioService = usuarioService;
    }

    private Usuario exigirProfessor(Authentication auth) {
        Usuario professor = (Usuario) auth.getPrincipal();
        if (professor.getTipo() != TipoUsuario.PROFESSOR && professor.getTipo() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operação disponível somente ao professor");
        }
        return professor;
    }

    @PostMapping
    public ResponseEntity<Curso> criarCurso(@Valid @RequestBody CursoRequest request, Authentication auth) {
        Usuario professor = exigirProfessor(auth);
        Curso curso = cursoService.criarCurso(request, professor);
        return ResponseEntity.ok(curso);
    }

    @GetMapping
    public ResponseEntity<List<Curso>> listarCursos() {
        return ResponseEntity.ok(cursoService.listarCursos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Curso> buscarCurso(@PathVariable Long id) {
        return ResponseEntity.ok(cursoService.buscarCurso(id));
    }

    @PostMapping("/matricular")
    public ResponseEntity<Void> matricular(@Valid @RequestBody MatriculaRequest request) {
        cursoService.matricularAluno(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/alunos")
    public ResponseEntity<List<UsuarioResponse>> listarAlunosDoCurso(@PathVariable Long id) {
        List<Usuario> alunos = cursoService.listarAlunosDoCurso(id);
        return ResponseEntity.ok(alunos.stream()
                .map(UsuarioResponse::fromEntity)
                .toList());
    }

    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<Curso>> listarCursosDoProfessor(@PathVariable Long professorId) {
        return ResponseEntity.ok(cursoService.listarCursosDoProfessor(professorId));
    }
}
