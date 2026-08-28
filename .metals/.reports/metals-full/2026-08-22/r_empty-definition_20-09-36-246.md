error id: file:///D:/devops-classroom-main-postgresql-fixed/devops-classroom-main/backend/src/main/java/com/devopsclassroom/controller/AulaController.java:com/devopsclassroom/entity/Usuario#
file:///D:/devops-classroom-main-postgresql-fixed/devops-classroom-main/backend/src/main/java/com/devopsclassroom/controller/AulaController.java
empty definition using pc, found symbol in pc: com/devopsclassroom/entity/Usuario#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 251
uri: file:///D:/devops-classroom-main-postgresql-fixed/devops-classroom-main/backend/src/main/java/com/devopsclassroom/controller/AulaController.java
text:
```scala
package com.devopsclassroom.controller;

import com.devopsclassroom.dto.AulaRequest;
import com.devopsclassroom.dto.UsuarioResponse;
import com.devopsclassroom.entity.Aula;
import com.devopsclassroom.entity.Mensagem;
import com.devopsclassroom.entity.@@Usuario;
import com.devopsclassroom.repository.MensagemRepository;
import com.devopsclassroom.service.AulaService;
import com.devopsclassroom.service.PresencaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aulas")
public class AulaController {
    private MensagemRepository mensagemRepository;
    private final AulaService aulaService;
    private final PresencaService presencaService;

    public AulaController(AulaService aulaService, PresencaService presencaService) {
        this.aulaService = aulaService;
        this.presencaService = presencaService;
    }

    @PostMapping
    public ResponseEntity<Aula> criarAula(@Valid @RequestBody AulaRequest request) {
        Aula aula = aulaService.criarAula(request);
        return ResponseEntity.ok(aula);
    }

    @GetMapping("/curso/{cursoId}")
    public ResponseEntity<List<Aula>> listarAulasDoCurso(@PathVariable Long cursoId) {
        return ResponseEntity.ok(aulaService.listarAulasPorCurso(cursoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Aula> buscarAula(@PathVariable Long id) {
        return ResponseEntity.ok(aulaService.buscarAula(id));
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Aula> iniciarAula(@PathVariable Long id) {
        return ResponseEntity.ok(aulaService.iniciarAula(id));
    }

    @PostMapping("/{id}/finalizar")
    public ResponseEntity<Aula> finalizarAula(@PathVariable Long id) {
        return ResponseEntity.ok(aulaService.finalizarAula(id));
    }

    @GetMapping("/em-andamento")
    public ResponseEntity<List<Aula>> listarAulasEmAndamento() {
        return ResponseEntity.ok(aulaService.listarAulasEmAndamento());
    }

    // Presença
    @PostMapping("/{aulaId}/presenca")
    public ResponseEntity<Void> registrarPresenca(@PathVariable Long aulaId, Authentication auth) {
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
    public ResponseEntity<List<Mensagem>> listarMensagensDaAula(@PathVariable Long aulaId) {
        List<Mensagem> mensagens = mensagemRepository.findByAulaIdOrderByCriadoEmAsc(aulaId);
        return ResponseEntity.ok(mensagens);
    }
    @GetMapping("/{aulaId}/presenca-verificar")
    public ResponseEntity<Boolean> verificarPresenca(@PathVariable Long aulaId, Authentication auth) {
        Usuario usuario = (Usuario) auth.getPrincipal();
        boolean presente = presencaService.verificarPresenca(usuario.getId(), aulaId);
        return ResponseEntity.ok(presente);
    }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: com/devopsclassroom/entity/Usuario#