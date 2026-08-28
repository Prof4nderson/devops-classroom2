package com.devopsclassroom.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Preserva o status original (401/403/404...). Sem isto, qualquer
    // ResponseStatusException virava 400 (Bad Request) e travava o login.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "Erro na requisição";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", reason, "message", reason));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : "Erro na requisição",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Erro na requisição"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno: " + ex.getMessage()));
    }
    private String getUsuarioIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("Usuário não autenticado no WebSocket.");
        }
        return principal.getName();
    }
}
