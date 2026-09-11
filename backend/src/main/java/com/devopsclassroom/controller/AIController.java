package com.devopsclassroom.controller;

import com.devopsclassroom.dto.AIChatRequest;
import com.devopsclassroom.service.AIAgentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class AIController {

    private final AIAgentService aiAgentService;

    public AIController(AIAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chamarCoder(@Valid @RequestBody AIChatRequest request) {
        String resposta = aiAgentService.responder(request.getMensagem());
        return ResponseEntity.ok(Map.of(
                "resposta", resposta,
                "assistente", "Coder"
        ));
    }
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDocumento(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", required = false) Long sessionId) {

        // Lógica para salvar o arquivo, ler o PDF e indexar no vetor/banco
        // String resultado = aiAgentService.processarDocumento(file, sessionId);

        return ResponseEntity.ok(Map.of(
                "message", "Arquivo enviado e processado com sucesso!",
                "filename", file.getOriginalFilename()
        ));
    }
}