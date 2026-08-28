package com.devopsclassroom.controller;

import com.devopsclassroom.dto.AulaRequest;
import com.devopsclassroom.dto.UsuarioResponse;
import com.devopsclassroom.entity.Aula;
import com.devopsclassroom.entity.Mensagem;
import com.devopsclassroom.entity.Usuario;
import com.devopsclassroom.repository.MensagemRepository;
import com.devopsclassroom.repository.MatriculaRepository;
import com.devopsclassroom.entity.StatusMatricula;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.devopsclassroom.service.AulaService;
import com.devopsclassroom.service.MensagemService;
import com.devopsclassroom.service.PresencaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {
    private final MensagemRepository mensagemRepository;
    private final AulaService aulaService;
    private final PresencaService presencaService;
    private final MatriculaRepository matriculaRepository;
    private final MensagemService mensagemService;

    public AulaController(AulaService aulaService, PresencaService presencaService, MensagemRepository mensagemRepository, MatriculaRepository matriculaRepository, MensagemService mensagemService) {
        this.aulaService = aulaService;
        this.presencaService = presencaService;
        this.mensagemRepository = mensagemRepository;
        this.matriculaRepository = matriculaRepository;
        this.mensagemService = mensagemService;
    }

    private void exigirProfessor(Authentication auth) {
        Usuario usuario = (Usuario) auth.getPrincipal();
        if (usuario.getTipo() != com.devopsclassroom.entity.TipoUsuario.PROFESSOR && usuario.getTipo() != com.devopsclassroom.entity.TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Gestão de aulas disponível somente ao professor");
        }
    }

    private void exigirAcesso(Aula aula, Authentication auth) {
        Usuario usuario = (Usuario) auth.getPrincipal();
        if (usuario.getTipo() == com.devopsclassroom.entity.TipoUsuario.PROFESSOR || usuario.getTipo() == com.devopsclassroom.entity.TipoUsuario.ADMIN) return;
        if (aula.getTurma() != null && !matriculaRepository.existsByUsuarioIdAndTurmaIdAndStatus(usuario.getId(), aula.getTurma().getId(), StatusMatricula.ATIVA)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Aula disponível somente aos alunos da turma correspondente");
        }
    }
    @PostMapping
    public ResponseEntity<Aula> criarAula(@Valid @RequestBody AulaRequest request, Authentication auth) {
        exigirProfessor(auth);
        Aula aula = aulaService.criarAula(request);
        return ResponseEntity.ok(aula);
    }

    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<Aula>> listarAulasDoCurso(@PathVariable Long cursoId) {
        return ResponseEntity.ok(aulaService.listarAulasPorCurso(cursoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Aula> buscarAula(@PathVariable Long id, Authentication auth) {
        Aula aula = aulaService.buscarAula(id);
        exigirAcesso(aula, auth);
        return ResponseEntity.ok(aula);
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Aula> iniciarAula(@PathVariable Long id, Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(aulaService.iniciarAula(id));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<Aula> finalizarAula(@PathVariable Long id, Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(aulaService.finalizarAula(id));
    }

    @GetMapping("/todas")
    public ResponseEntity<List<Aula>> listarTodas(Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(aulaService.listarTodas());
    }

    @GetMapping("/turma/{turmaId}")
    public ResponseEntity<List<Aula>> listarAulasDaTurma(@PathVariable Long turmaId) {
        return ResponseEntity.ok(aulaService.listarAulasPorTurma(turmaId));
    }

    @GetMapping("/em-andamento")
    public ResponseEntity<List<Aula>> listarAulasEmAndamento() {
        return ResponseEntity.ok(aulaService.listarAulasEmAndamento());
    }

    // Presença
    @PostMapping("/{aulaId}/presenca")
    public ResponseEntity<Void> registrarPresenca(@PathVariable Long aulaId, Authentication auth) {
        Aula aula = aulaService.buscarAula(aulaId);
        exigirAcesso(aula, auth);
        Usuario usuario = (Usuario) auth.getPrincipal();
        presencaService.registrarPresenca(usuario.getId(), aulaId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{aulaId}/presentes")
    public ResponseEntity<List<UsuarioResponse>> listarPresentes(@PathVariable Long aulaId) {
        List<Usuario> presentes = presencaService.listarPresentes(aulaId);
        return ResponseEntity.ok(presentes.stream()
                .map(UsuarioResponse::fromEntity)
                .toList());
    }
    @GetMapping("/{aulaId}/mensagens")
    public ResponseEntity<List<com.devopsclassroom.dto.MensagemResponse>> listarMensagensDaAula(@PathVariable Long aulaId, Authentication auth) {
        Aula aula = aulaService.buscarAula(aulaId);
        exigirAcesso(aula, auth);
        return ResponseEntity.ok(mensagemService.listarRespostasDaAula(aulaId));
    }
    @GetMapping("/{aulaId}/presenca-verificar")
    public ResponseEntity<Boolean> verificarPresenca(@PathVariable Long aulaId, Authentication auth) {
        Usuario usuario = (Usuario) auth.getPrincipal();
        boolean presente = presencaService.verificarPresenca(usuario.getId(), aulaId);
        return ResponseEntity.ok(presente);
    }
}
