package com.devopsclassroom.service;

import com.devopsclassroom.dto.QuizRequest;
import com.devopsclassroom.dto.RespostaQuizRequest;
import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final RespostaQuizRepository respostaQuizRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public QuizService(QuizRepository quizRepository, RespostaQuizRepository respostaQuizRepository,
                      AulaRepository aulaRepository, UsuarioRepository usuarioRepository,
                      ObjectMapper objectMapper) {
        this.quizRepository = quizRepository;
        this.respostaQuizRepository = respostaQuizRepository;
        this.aulaRepository = aulaRepository;
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Quiz criarQuiz(QuizRequest request, Long professorId) {
        Aula aula = aulaRepository.findById(request.getAulaId())
                .orElseThrow(() -> new RuntimeException("Aula não encontrada"));
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new RuntimeException("Professor não encontrado"));

        Quiz quiz = new Quiz();
        quiz.setAula(aula);
        quiz.setProfessor(professor);
        quiz.setPergunta(request.getPergunta());
        try {
            quiz.setOpcoes(objectMapper.writeValueAsString(request.getOpcoes()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao processar opções");
        }
        quiz.setRespostaCorreta(request.getRespostaCorreta());
        quiz.setTempoLimiteSegundos(request.getTempoLimiteSegundos());

        return quizRepository.save(quiz);
    }

    @Transactional
    public RespostaQuiz responderQuiz(RespostaQuizRequest request, Long usuarioId) {
        Quiz quiz = quizRepository.findByIdAndStatus(request.getQuizId(), StatusQuiz.ATIVO)
                .orElseThrow(() -> new RuntimeException("Quiz não encontrado ou já finalizado"));

        if (respostaQuizRepository.existsByQuizIdAndUsuarioId(request.getQuizId(), usuarioId)) {
            throw new RuntimeException("Você já respondeu este quiz");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        RespostaQuiz resposta = new RespostaQuiz();
        resposta.setQuiz(quiz);
        resposta.setUsuario(usuario);
        resposta.setRespostaSelecionada(request.getRespostaSelecionada());
        boolean correta = request.getRespostaSelecionada().equals(quiz.getRespostaCorreta());
        resposta.setEstaCorreta(correta);

        return respostaQuizRepository.save(resposta);
    }

    @Transactional
    public Quiz finalizarQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz não encontrado"));
        quiz.setStatus(StatusQuiz.FINALIZADO);
        quiz.setFinalizadoEm(LocalDateTime.now());
        return quizRepository.save(quiz);
    }

    public Quiz buscar(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz não encontrado"));
    }

    public List<Quiz> listarQuizzesDaAula(Long aulaId) {
        return quizRepository.findByAulaId(aulaId);
    }

    public List<RespostaQuiz> listarRespostasDoQuiz(Long quizId) {
        return respostaQuizRepository.findByQuizId(quizId);
    }

    public List<String> opcoesDe(Quiz quiz) {
        if (quiz.getOpcoes() == null || quiz.getOpcoes().isBlank()) return List.of();
        try {
            return objectMapper.readValue(quiz.getOpcoes(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Estrutura plana do quiz, sem proxies lazy do Hibernate. */
    public Map<String, Object> mapQuiz(Quiz quiz) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", quiz.getId());
        m.put("quizId", quiz.getId());
        m.put("aulaId", quiz.getAula() != null ? quiz.getAula().getId() : null);
        m.put("pergunta", quiz.getPergunta());
        m.put("opcoes", opcoesDe(quiz));
        m.put("respostaCorreta", quiz.getRespostaCorreta());
        m.put("tempoLimiteSegundos", quiz.getTempoLimiteSegundos());
        m.put("status", quiz.getStatus() != null ? quiz.getStatus().name() : null);
        m.put("professorId", quiz.getProfessor() != null ? quiz.getProfessor().getId() : null);
        m.put("professorNome", quiz.getProfessor() != null ? quiz.getProfessor().getNome() : null);
        m.put("criadoEm", quiz.getCriadoEm() != null ? quiz.getCriadoEm().toString() : null);
        m.put("finalizadoEm", quiz.getFinalizadoEm() != null ? quiz.getFinalizadoEm().toString() : null);
        return m;
    }

    /** Resultado consolidado: contagem por opção, acertos e lista de respostas. */
    public Map<String, Object> resultado(Long quizId) {
        Quiz quiz = buscar(quizId);
        List<RespostaQuiz> respostas = respostaQuizRepository.findByQuizId(quizId);
        List<String> opcoes = opcoesDe(quiz);

        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (String opcao : opcoes) contagem.put(opcao, 0);
        int acertos = 0;
        List<Map<String, Object>> detalhes = new ArrayList<>();
        for (RespostaQuiz r : respostas) {
            contagem.merge(r.getRespostaSelecionada(), 1, Integer::sum);
            if (Boolean.TRUE.equals(r.getEstaCorreta())) acertos++;
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("usuarioId", r.getUsuario() != null ? r.getUsuario().getId() : null);
            d.put("usuarioNome", r.getUsuario() != null ? r.getUsuario().getNome() : null);
            d.put("resposta", r.getRespostaSelecionada());
            d.put("correta", Boolean.TRUE.equals(r.getEstaCorreta()));
            detalhes.add(d);
        }

        Map<String, Object> out = new LinkedHashMap<>(mapQuiz(quiz));
        out.put("tipo", "QUIZ_RESULT");
        out.put("totalRespostas", respostas.size());
        out.put("acertos", acertos);
        out.put("percentualAcerto", respostas.isEmpty() ? 0 : Math.round((acertos * 1000.0) / respostas.size()) / 10.0);
        out.put("contagem", contagem);
        out.put("respostas", detalhes);
        return out;
    }

    /** Resultado seguro para transmissão em sala: sem gabarito enquanto o quiz está aberto. */
    public Map<String, Object> resultadoPublico(Long quizId) {
        Map<String, Object> out = resultado(quizId);
        if (!"FINALIZADO".equals(String.valueOf(out.get("status")))) {
            out.remove("respostaCorreta");
            out.remove("respostas");
        }
        return out;
    }
}
