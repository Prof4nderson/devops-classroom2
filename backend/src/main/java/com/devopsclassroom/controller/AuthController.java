package com.devopsclassroom.controller;

import com.devopsclassroom.dto.AuthRequest;
import com.devopsclassroom.dto.AuthResponse;
import com.devopsclassroom.dto.UsuarioRequest;
import com.devopsclassroom.dto.UsuarioResponse;
import com.devopsclassroom.service.UsuarioService;
import com.devopsclassroom.entity.TipoUsuario;
import com.devopsclassroom.entity.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = usuarioService.autenticar(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse response = usuarioService.criarUsuario(request);
        return ResponseEntity.ok(response);
    }

    private void exigirProfessor(Authentication auth) {
        Usuario usuario = (Usuario) auth.getPrincipal();
        if (usuario.getTipo() != TipoUsuario.PROFESSOR && usuario.getTipo() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CRUD disponível somente ao professor");
        }
    }

    @PostMapping("/users")
    public ResponseEntity<UsuarioResponse> criarUsuarioAdministrativo(@Valid @RequestBody UsuarioRequest request, Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(usuarioService.criarUsuarioAdministrativo(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios(Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(usuarioService.listarUsuarios());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UsuarioResponse> buscarUsuario(@PathVariable Long id, Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> excluirUsuario(@PathVariable Long id, Authentication auth) {
        exigirProfessor(auth);
        usuarioService.excluirUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<UsuarioResponse> atualizarUsuario(@PathVariable Long id,
                                                             @Valid @RequestBody UsuarioRequest request,
                                                             Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(usuarioService.atualizarUsuario(id, request));
    }

    @GetMapping("/users/alunos/search")
    public ResponseEntity<List<UsuarioResponse>> buscarAlunos(@RequestParam String nome, Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(usuarioService.buscarAlunos(nome));
    }

    @GetMapping("/users/professores")
    public ResponseEntity<List<UsuarioResponse>> buscarProfessores(Authentication auth) {
        exigirProfessor(auth);
        return ResponseEntity.ok(usuarioService.buscarProfessores());
    }
}
