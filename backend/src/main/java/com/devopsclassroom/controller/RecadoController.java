package com.devopsclassroom.controller;

import com.devopsclassroom.entity.TipoUsuario;
import com.devopsclassroom.entity.Usuario;
import com.devopsclassroom.repository.UsuarioRepository;
import com.devopsclassroom.service.PresencaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Recado individual do professor para um aluno presente na aula.
 * Entregue em tempo real por WebSocket na fila privada do aluno
 * (/user/queue/avisos) — nenhum outro participante recebe.
 */
@RestController
@RequestMapping("/api/recados")
public class RecadoController {

    private static final Set<String> TONS = Set.of("INFO", "ELOGIO", "ATENCAO");

    private final SimpMessagingTemplate messaging;
    private final UsuarioRepository usuarios;
    private final PresencaService presencaService;

    public RecadoController(SimpMessagingTemplate messaging, UsuarioRepository usuarios, PresencaService presencaService) {
        this.messaging = messaging;
        this.usuarios = usuarios;
        this.presencaService = presencaService;
    }

    @PostMapping("/aula/{aulaId}")
    public ResponseEntity<Map<String, Object>> enviar(@PathVariable Long aulaId,
                                                      @RequestBody Map<String, Object> body,
                                                      Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Usuario professor)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }
        if (professor.getTipo() != TipoUsuario.PROFESSOR && professor.getTipo() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o professor pode enviar recados");
        }

        Object alunoIdRaw = body.get("alunoId");
        String mensagem = body.get("mensagem") != null ? body.get("mensagem").toString().trim() : "";
        if (alunoIdRaw == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o aluno");
        if (mensagem.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva a mensagem");
        if (mensagem.length() > 500) mensagem = mensagem.substring(0, 500);

        Long alunoId = Long.valueOf(alunoIdRaw.toString());
        Usuario aluno = usuarios.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));

        boolean presente = presencaService.listarPresentes(aulaId).stream()
                .anyMatch(u -> u.getId().equals(alunoId));
        if (!presente) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O aluno não está presente nesta aula");
        }

        String tom = body.get("tom") != null ? body.get("tom").toString().toUpperCase(Locale.ROOT) : "INFO";
        if (!TONS.contains(tom)) tom = "INFO";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipo", "RECADO");
        payload.put("aulaId", aulaId);
        payload.put("mensagem", mensagem);
        payload.put("tom", tom);
        payload.put("deNome", professor.getNome() != null ? professor.getNome() : professor.getLogin());
        payload.put("enviadoEm", LocalDateTime.now().toString());

        messaging.convertAndSendToUser(aluno.getLogin(), "/queue/avisos", payload);

        return ResponseEntity.ok(payload);
    }
}
