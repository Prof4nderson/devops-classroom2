package com.devopsclassroom.controller;

import com.devopsclassroom.dto.QuizRequest;
import com.devopsclassroom.dto.RespostaQuizRequest;
import com.devopsclassroom.entity.Quiz;
import com.devopsclassroom.entity.TipoUsuario;
import com.devopsclassroom.entity.Usuario;
import com.devopsclassroom.repository.UsuarioRepository;
import com.devopsclassroom.service.QuizService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ciclo de vida do quiz em tempo real:
 * o professor publica em /app/quiz/{aulaId}/create e os alunos respondem em
 * /app/quiz/{aulaId}/respond. Tudo é transmitido em /topic/quiz/{aulaId}.
 */
@Controller
public class QuizWebSocketController {

    private final QuizService quizService;
    private final UsuarioRepository usuarioRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public QuizWebSocketController(QuizService quizService, UsuarioRepository usuarioRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.quizService = quizService;
        this.usuarioRepository = usuarioRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private Optional<Usuario> usuarioDe(Principal principal) {
        if (principal == null) return Optional.empty();
        return usuarioRepository.findByLogin(principal.getName())
                .or(() -> usuarioRepository.findByEmail(principal.getName()));
    }

    @SuppressWarnings("unchecked")
    @MessageMapping("/quiz/{aulaId}/create")
    public void criar(@DestinationVariable Long aulaId, Map<String, Object> body, Principal principal) {
        Usuario professor = usuarioDe(principal).orElse(null);
        if (professor == null) return;
        if (professor.getTipo() != TipoUsuario.PROFESSOR && professor.getTipo() != TipoUsuario.ADMIN) return;

        Long quizId = body.get("quizId") instanceof Number n ? n.longValue() : null;
        Quiz quiz = null;

        if (quizId != null) {
            try {
                quiz = quizService.buscar(quizId);
            } catch (RuntimeException ignored) {
                quiz = null;
            }
        }

        if (quiz == null) {
            // Quiz ainda não persistido (fallback do cliente): grava agora.
            QuizRequest request = new QuizRequest();
            request.setAulaId(aulaId);
            request.setPergunta(String.valueOf(body.getOrDefault("pergunta", "")));
            Object opcoes = body.get("opcoes");
            List<String> lista = opcoes instanceof List<?> l ? (List<String>) l : List.of();
            if (lista.size() < 2 || request.getPergunta().isBlank()) return;
            request.setOpcoes(lista);
            Object correta = body.get("respostaCorreta");
            request.setRespostaCorreta(correta != null ? String.valueOf(correta) : lista.get(0));
            Object tempo = body.get("tempoLimiteSegundos");
            request.setTempoLimiteSegundos(tempo instanceof Number t ? t.intValue() : 30);
            quiz = quizService.criarQuiz(request, professor.getId());
        }

        Map<String, Object> payload = new LinkedHashMap<>(quizService.mapQuiz(quiz));
        payload.put("tipo", "QUIZ_CREATED");
        payload.remove("respostaCorreta");
        messagingTemplate.convertAndSend("/topic/quiz/" + aulaId, payload);
    }

    @MessageMapping("/quiz/{aulaId}/respond")
    public void responder(@DestinationVariable Long aulaId, Map<String, Object> body, Principal principal) {
        Usuario usuario = usuarioDe(principal).orElse(null);
        if (usuario == null) return;

        Long quizId = body.get("quizId") instanceof Number n ? n.longValue() : null;
        Object resposta = body.get("resposta") != null ? body.get("resposta") : body.get("respostaSelecionada");
        if (quizId == null || resposta == null) return;

        RespostaQuizRequest request = new RespostaQuizRequest();
        request.setQuizId(quizId);
        request.setRespostaSelecionada(String.valueOf(resposta));

        Map<String, Object> retornoPessoal = new LinkedHashMap<>();
        retornoPessoal.put("tipo", "QUIZ_ANSWERED");
        retornoPessoal.put("quizId", quizId);
        try {
            var salva = quizService.responderQuiz(request, usuario.getId());
            retornoPessoal.put("correta", Boolean.TRUE.equals(salva.getEstaCorreta()));
            retornoPessoal.put("resposta", salva.getRespostaSelecionada());
            messagingTemplate.convertAndSend("/topic/quiz/" + aulaId, quizService.resultadoPublico(quizId));
        } catch (RuntimeException e) {
            retornoPessoal.put("erro", e.getMessage());
        }
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/quiz", retornoPessoal);
    }
}
