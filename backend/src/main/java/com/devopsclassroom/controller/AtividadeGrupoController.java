package com.devopsclassroom.controller;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Atividades em grupo: cadastro (assunto, tarefas, data de entrega, participantes)
 * e divisão dos alunos em grupos por sorteio.
 * Criação/edição são exclusivas de PROFESSOR/ADMIN; alunos apenas consultam.
 */
@RestController
@RequestMapping("/api/atividades-grupo")
@Transactional
public class AtividadeGrupoController {

    private final TrabalhoRepository trabalhos;
    private final EquipeRepository equipes;
    private final TurmaRepository turmas;
    private final UsuarioRepository usuarios;
    private final MatriculaRepository matriculas;
    private final ObjectMapper mapper = new ObjectMapper();

    public AtividadeGrupoController(TrabalhoRepository trabalhos, EquipeRepository equipes, TurmaRepository turmas,
                                    UsuarioRepository usuarios, MatriculaRepository matriculas) {
        this.trabalhos = trabalhos;
        this.equipes = equipes;
        this.turmas = turmas;
        this.usuarios = usuarios;
        this.matriculas = matriculas;
    }

    // ------------------------------------------------------------- helpers

    private Usuario autenticado(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Usuario user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }
        return user;
    }

    private Usuario professor(Authentication auth) {
        Usuario user = autenticado(auth);
        if (user.getTipo() != TipoUsuario.PROFESSOR && user.getTipo() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação permitida somente ao professor");
        }
        return user;
    }

    private List<String> tarefasDe(Trabalho t) {
        try {
            String json = t.getChecklistJson();
            if (json == null || json.isBlank()) return List.of();
            return Arrays.asList(mapper.readValue(json, String[].class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String comoJson(List<String> tarefas) {
        try {
            return mapper.writeValueAsString(tarefas == null ? List.of() : tarefas);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, Object> aluno(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("nome", u.getNome() != null ? u.getNome() : u.getLogin());
        return m;
    }

    private Map<String, Object> mapear(Trabalho t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("titulo", t.getTitulo());
        m.put("assunto", t.getAssunto());
        m.put("descricao", t.getDescricao());
        m.put("pontuacaoMaxima", t.getPontuacaoMaxima());
        m.put("prazoEntrega", t.getPrazoEntrega() != null ? t.getPrazoEntrega().toString() : null);
        m.put("turmaId", t.getTurma() != null ? t.getTurma().getId() : null);
        m.put("turmaNome", t.getTurma() != null
                ? (t.getTurma().getNome() != null ? t.getTurma().getNome() : t.getTurma().getCodigo()) : null);
        m.put("professorNome", t.getProfessor() != null ? t.getProfessor().getNome() : null);
        m.put("tarefas", tarefasDe(t));
        m.put("participantes", t.getParticipantes().stream().map(this::aluno).toList());

        List<Map<String, Object>> grupos = new ArrayList<>();
        for (Equipe e : equipes.findByTrabalhoId(t.getId())) {
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("id", e.getId());
            g.put("nome", e.getNome());
            g.put("integrantes", e.getAlunos().stream().map(this::aluno).toList());
            grupos.add(g);
        }
        grupos.sort(Comparator.comparing(g -> String.valueOf(g.get("nome"))));
        m.put("grupos", grupos);
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<Long> idsDe(Object valor) {
        if (!(valor instanceof List<?> lista)) return new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (Object o : lista) {
            if (o instanceof Number n) ids.add(n.longValue());
            else if (o != null) try { ids.add(Long.parseLong(o.toString())); } catch (NumberFormatException ignored) { }
        }
        return ids;
    }

    private List<String> textosDe(Object valor) {
        if (!(valor instanceof List<?> lista)) return new ArrayList<>();
        List<String> textos = new ArrayList<>();
        for (Object o : lista) {
            if (o != null && !o.toString().isBlank()) textos.add(o.toString().trim());
        }
        return textos;
    }

    // ------------------------------------------------------------- consultas

    /** Alunos matriculados na turma — base de participantes da atividade. */
    @GetMapping("/turmas/{turmaId}/alunos")
    public ResponseEntity<List<Map<String, Object>>> alunosDaTurma(@PathVariable Long turmaId, Authentication auth) {
        professor(auth);
        List<Map<String, Object>> lista = matriculas.findAll().stream()
                .filter(m -> m.getTurma() != null && turmaId.equals(m.getTurma().getId()))
                .filter(m -> m.getUsuario() != null && m.getUsuario().getTipo() == TipoUsuario.ALUNO)
                .map(m -> aluno(m.getUsuario()))
                .distinct()
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listar(@RequestParam(required = false) Long turmaId,
                                                            Authentication auth) {
        Usuario user = autenticado(auth);
        List<Trabalho> lista = (turmaId != null ? trabalhos.findByTurmaId(turmaId) : trabalhos.findAll())
                .stream().filter(Trabalho::isTrabalhoEquipe).toList();

        if (user.getTipo() == TipoUsuario.ALUNO) {
            Set<Long> minhasTurmas = new HashSet<>();
            matriculas.findByUsuarioId(user.getId()).forEach(m -> {
                if (m.getTurma() != null) minhasTurmas.add(m.getTurma().getId());
            });
            lista = lista.stream()
                    .filter(t -> t.getTurma() != null && minhasTurmas.contains(t.getTurma().getId()))
                    .toList();
        }
        return ResponseEntity.ok(lista.stream().map(this::mapear).toList());
    }

    // ------------------------------------------------------------- escrita

    @PostMapping
    public ResponseEntity<Map<String, Object>> criar(@RequestBody Map<String, Object> body, Authentication auth) {
        Usuario prof = professor(auth);

        Object turmaIdRaw = body.get("turmaId");
        if (turmaIdRaw == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a turma");
        Turma turma = turmas.findById(Long.valueOf(turmaIdRaw.toString()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        Trabalho t = new Trabalho();
        t.setTitulo(String.valueOf(body.getOrDefault("titulo", "Atividade em grupo")));
        t.setAssunto(body.get("assunto") != null ? body.get("assunto").toString() : null);
        t.setDescricao(body.get("descricao") != null ? body.get("descricao").toString() : null);
        t.setTrabalhoEquipe(true);
        t.setTurma(turma);
        t.setProfessor(prof);
        if (body.get("pontuacaoMaxima") != null) {
            t.setPontuacaoMaxima(Integer.valueOf(body.get("pontuacaoMaxima").toString()));
        }
        if (body.get("prazoEntrega") != null && !body.get("prazoEntrega").toString().isBlank()) {
            t.setPrazoEntrega(LocalDateTime.parse(body.get("prazoEntrega").toString()));
        }
        t.setChecklistJson(comoJson(textosDe(body.get("tarefas"))));
        t.setParticipantes(new LinkedHashSet<>(usuarios.findAllById(idsDe(body.get("participantes")))));

        return ResponseEntity.ok(mapear(trabalhos.save(t)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> atualizar(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                         Authentication auth) {
        professor(auth);
        Trabalho t = trabalhos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));

        if (body.get("titulo") != null) t.setTitulo(body.get("titulo").toString());
        if (body.get("assunto") != null) t.setAssunto(body.get("assunto").toString());
        if (body.get("descricao") != null) t.setDescricao(body.get("descricao").toString());
        if (body.get("pontuacaoMaxima") != null) t.setPontuacaoMaxima(Integer.valueOf(body.get("pontuacaoMaxima").toString()));
        if (body.get("prazoEntrega") != null && !body.get("prazoEntrega").toString().isBlank()) {
            t.setPrazoEntrega(LocalDateTime.parse(body.get("prazoEntrega").toString()));
        }
        if (body.get("tarefas") != null) t.setChecklistJson(comoJson(textosDe(body.get("tarefas"))));
        if (body.get("participantes") != null) {
            t.setParticipantes(new LinkedHashSet<>(usuarios.findAllById(idsDe(body.get("participantes")))));
        }
        return ResponseEntity.ok(mapear(trabalhos.save(t)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, Authentication auth) {
        professor(auth);
        equipes.deleteByTrabalhoId(id);
        trabalhos.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------- sorteio

    /**
     * Sorteia os participantes em grupos. Informe {@code tamanhoGrupo} (padrão 3)
     * ou {@code quantidadeGrupos}. Grupos anteriores da atividade são substituídos.
     */
    @PostMapping("/{id}/sorteio")
    public ResponseEntity<Map<String, Object>> sortear(@PathVariable Long id,
                                                       @RequestParam(required = false) Integer tamanhoGrupo,
                                                       @RequestParam(required = false) Integer quantidadeGrupos,
                                                       Authentication auth) {
        professor(auth);
        Trabalho t = trabalhos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));

        List<Usuario> sorteaveis = new ArrayList<>(t.getParticipantes());
        if (sorteaveis.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adicione participantes antes de sortear");
        }
        Collections.shuffle(sorteaveis, new Random());

        int totalGrupos;
        if (quantidadeGrupos != null && quantidadeGrupos > 0) {
            totalGrupos = Math.min(quantidadeGrupos, sorteaveis.size());
        } else {
            int tamanho = (tamanhoGrupo != null && tamanhoGrupo > 0) ? tamanhoGrupo : 3;
            totalGrupos = (int) Math.ceil(sorteaveis.size() / (double) tamanho);
        }
        if (totalGrupos < 1) totalGrupos = 1;

        equipes.deleteByTrabalhoId(id);
        equipes.flush();

        List<List<Usuario>> distribuicao = new ArrayList<>();
        for (int i = 0; i < totalGrupos; i++) distribuicao.add(new ArrayList<>());
        for (int i = 0; i < sorteaveis.size(); i++) {
            distribuicao.get(i % totalGrupos).add(sorteaveis.get(i)); // distribuição equilibrada
        }

        for (int i = 0; i < distribuicao.size(); i++) {
            Equipe equipe = new Equipe();
            equipe.setNome(String.format("Grupo %02d", i + 1));
            equipe.setTurma(t.getTurma());
            equipe.setTrabalho(t);
            equipe.setAlunos(new LinkedHashSet<>(distribuicao.get(i)));
            equipe.setChecklistJson(t.getChecklistJson());
            equipes.save(equipe);
        }

        return ResponseEntity.ok(mapear(t));
    }
}
