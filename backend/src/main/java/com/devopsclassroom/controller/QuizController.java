package com.devopsclassroom.controller;

import com.devopsclassroom.dto.QuizRequest;
import com.devopsclassroom.dto.RespostaQuizRequest;
import com.devopsclassroom.entity.Quiz;
import com.devopsclassroom.entity.RespostaQuiz;
import com.devopsclassroom.entity.TipoUsuario;
import com.devopsclassroom.entity.Usuario;
import com.devopsclassroom.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;
    private final SimpMessagingTemplate messagingTemplate;

    public QuizController(QuizService quizService, SimpMessagingTemplate messagingTemplate) {
        this.quizService = quizService;
        this.messagingTemplate = messagingTemplate;
    }

    private Usuario autenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Usuario user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }
        return user;
    }

    private Usuario professor(Authentication auth) {
        Usuario user = autenticado(auth);
        if (user.getTipo() != TipoUsuario.PROFESSOR && user.getTipo() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o professor pode gerenciar quizzes");
        }
        return user;
    }

    /** Cria o quiz e já transmite para a sala da aula. */
    @PostMapping
    public Map<String, Object> criarQuiz(@Valid @RequestBody QuizRequest request, Authentication auth) {
        Usuario professor = professor(auth);
        Quiz quiz = quizService.criarQuiz(request, professor.getId());
        Map<String, Object> payload = new LinkedHashMap<>(quizService.mapQuiz(quiz));
        payload.put("tipo", "QUIZ_CREATED");
        payload.remove("respostaCorreta"); // alunos não recebem o gabarito
        messagingTemplate.convertAndSend("/topic/quiz/" + request.getAulaId(), payload);
        return quizService.mapQuiz(quiz);
    }

    @PostMapping("/responder")
    public Map<String, Object> responderQuiz(@Valid @RequestBody RespostaQuizRequest request, Authentication auth) {
        Usuario usuario = autenticado(auth);
        RespostaQuiz resposta = quizService.responderQuiz(request, usuario.getId());
        Long aulaId = resposta.getQuiz().getAula() != null ? resposta.getQuiz().getAula().getId() : null;
        if (aulaId != null) {
            messagingTemplate.convertAndSend("/topic/quiz/" + aulaId, quizService.resultadoPublico(request.getQuizId()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", resposta.getId());
        out.put("quizId", request.getQuizId());
        out.put("respostaSelecionada", resposta.getRespostaSelecionada());
        out.put("correta", Boolean.TRUE.equals(resposta.getEstaCorreta()));
        return out;
    }

    @PostMapping("/{id}/finalizar")
    public Map<String, Object> finalizarQuiz(@PathVariable Long id, Authentication auth) {
        professor(auth);
        Quiz quiz = quizService.finalizarQuiz(id);
        Map<String, Object> resultado = quizService.resultado(id);
        resultado.put("tipo", "QUIZ_CLOSED");
        if (quiz.getAula() != null) {
            messagingTemplate.convertAndSend("/topic/quiz/" + quiz.getAula().getId(), resultado);
        }
        return resultado;
    }

    @GetMapping("/aula/{aulaId}")
    public List<Map<String, Object>> listarQuizzesDaAula(@PathVariable Long aulaId, Authentication auth) {
        Usuario user = autenticado(auth);
        boolean docente = user.getTipo() == TipoUsuario.PROFESSOR || user.getTipo() == TipoUsuario.ADMIN;
        return quizService.listarQuizzesDaAula(aulaId).stream().map(q -> {
            Map<String, Object> m = quizService.mapQuiz(q);
            if (!docente) m.remove("respostaCorreta");
            return m;
        }).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> buscar(@PathVariable Long id, Authentication auth) {
        Usuario user = autenticado(auth);
        Map<String, Object> m = quizService.mapQuiz(quizService.buscar(id));
        if (user.getTipo() == TipoUsuario.ALUNO) m.remove("respostaCorreta");
        return m;
    }

    @GetMapping("/{id}/resultado")
    public Map<String, Object> resultado(@PathVariable Long id, Authentication auth) {
        autenticado(auth);
        return quizService.resultado(id);
    }

    @GetMapping("/{id}/respostas")
    public List<Map<String, Object>> listarRespostas(@PathVariable Long id, Authentication auth) {
        professor(auth);
        return quizService.listarRespostasDoQuiz(id).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("usuarioId", r.getUsuario() != null ? r.getUsuario().getId() : null);
            m.put("usuarioNome", r.getUsuario() != null ? r.getUsuario().getNome() : null);
            m.put("resposta", r.getRespostaSelecionada());
            m.put("correta", Boolean.TRUE.equals(r.getEstaCorreta()));
            m.put("dataResposta", r.getDataResposta() != null ? r.getDataResposta().toString() : null);
            return m;
        }).toList();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> erro(RuntimeException e) {
        if (e instanceof ResponseStatusException rse) throw rse;
        return Map.of("erro", e.getMessage() != null ? e.getMessage() : "Erro ao processar o quiz");
    }
}
