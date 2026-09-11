package com.devopsclassroom.controller;

import com.devopsclassroom.dto.AIChatRequest;
import com.devopsclassroom.service.AIAgentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class AIController {

    private final AIAgentService aiAgentService;

    public AIController(AIAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @PostMapping(value = "/chat", consumes = "text/plain")
    public ResponseEntity<Map<String, String>> chamarCoder(@RequestBody String mensagem) {
        String resposta = aiAgentService.responder(mensagem);
        return ResponseEntity.ok(Map.of(
                "resposta", resposta,
                "assistente", "Coder"
        ));
    }
}
