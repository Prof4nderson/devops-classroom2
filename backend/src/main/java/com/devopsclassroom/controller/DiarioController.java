package com.devopsclassroom.controller;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import com.devopsclassroom.service.AulaService;
import com.devopsclassroom.service.PresencaService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Diário de classe do professor: presença, conteúdo ministrado,
 * avaliações e média por aluno. O aluno enxerga apenas o próprio boletim.
 */
@RestController
@RequestMapping("/api/diario")
@Transactional
public class DiarioController {

    private final TurmaRepository turmas;
    private final AulaRepository aulas;
    private final MatriculaRepository matriculas;
    private final PresencaRepository presencas;
    private final AvaliacaoRepository avaliacoes;
    private final UsuarioRepository usuarios;
    private final PresencaService presencaService;
    private final AulaService aulaService;

    public DiarioController(TurmaRepository turmas, AulaRepository aulas, MatriculaRepository matriculas,
                            PresencaRepository presencas, AvaliacaoRepository avaliacoes,
                            UsuarioRepository usuarios, PresencaService presencaService, AulaService aulaService) {
        this.turmas = turmas;
        this.aulas = aulas;
        this.matriculas = matriculas;
        this.presencas = presencas;
        this.avaliacoes = avaliacoes;
        this.usuarios = usuarios;
        this.presencaService = presencaService;
        this.aulaService = aulaService;
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Diário disponível somente ao professor");
        }
        return user;
    }

    private static ResponseStatusException naoEncontrado(String recurso) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " não encontrado");
    }

    private static String str(Map<String, Object> body, String chave) {
        Object v = body.get(chave);
        return v == null || String.valueOf(v).isBlank() ? null : String.valueOf(v).trim();
    }

    private static Long id(Map<String, Object> body, String chave) {
        Object v = body.get(chave);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Long.valueOf(String.valueOf(v).trim());
    }

    private static Double num(Map<String, Object> body, String chave) {
        Object v = body.get(chave);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Double.valueOf(String.valueOf(v).replace(",", "."));
    }

    private Map<String, Object> mapAula(Aula a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("titulo", a.getTitulo());
        m.put("descricao", a.getDescricao());
        m.put("dataAula", a.getDataAula() != null ? a.getDataAula().toString() : null);
        m.put("duracao", a.getDuracao());
        m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
        m.put("iniciadaEm", a.getIniciadaEm() != null ? a.getIniciadaEm().toString() : null);
        m.put("finalizadaEm", a.getFinalizadaEm() != null ? a.getFinalizadaEm().toString() : null);
        m.put("conteudoMinistrado", a.getConteudoMinistrado());
        m.put("observacoes", a.getObservacoes());
        m.put("cursoId", a.getCurso() != null ? a.getCurso().getId() : null);
        m.put("turmaId", a.getTurma() != null ? a.getTurma().getId() : null);
        return m;
    }

    private Map<String, Object> mapTurma(Turma t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("codigo", t.getCodigo());
        m.put("nome", t.getNome());
        m.put("curso", t.getCurso() != null ? t.getCurso().getNome() : null);
        m.put("cursoId", t.getCurso() != null ? t.getCurso().getId() : null);
        m.put("periodo", t.getPeriodo() != null ? t.getPeriodo().getNome() : null);
        m.put("professor", t.getProfessor() != null ? t.getProfessor().getNome() : null);
        return m;
    }

    private Map<String, Object> mapAvaliacao(Avaliacao a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("alunoId", a.getAluno() != null ? a.getAluno().getId() : null);
        m.put("alunoNome", a.getAluno() != null ? a.getAluno().getNome() : null);
        m.put("turmaId", a.getTurma() != null ? a.getTurma().getId() : null);
        m.put("aulaId", a.getAula() != null ? a.getAula().getId() : null);
        m.put("titulo", a.getTitulo());
        m.put("tipo", a.getTipo());
        m.put("nota", a.getNota());
        m.put("peso", a.getPeso());
        m.put("criadoEm", a.getCriadoEm() != null ? a.getCriadoEm().toString() : null);
        return m;
    }

    private static double media(List<Avaliacao> notas) {
        double somaPesos = 0, soma = 0;
        for (Avaliacao a : notas) {
            double peso = a.getPeso() != null ? a.getPeso() : 1.0;
            soma += a.getNota() * peso;
            somaPesos += peso;
        }
        return somaPesos == 0 ? 0 : Math.round((soma / somaPesos) * 100) / 100.0;
    }

    // -------------------------------------------------------------- turmas

    @GetMapping("/turmas")
    public List<Map<String, Object>> listarTurmas(Authentication auth) {
        Usuario user = professor(auth);
        List<Turma> lista = user.getTipo() == TipoUsuario.ADMIN
                ? turmas.findAll()
                : turmas.findAll().stream()
                    .filter(t -> t.getProfessor() == null || Objects.equals(t.getProfessor().getId(), user.getId()))
                    .toList();
        return lista.stream().map(this::mapTurma).toList();
    }

    /** Diário completo da turma: aulas, chamada, avaliações e médias. */
    @GetMapping("/turma/{turmaId}")
    public Map<String, Object> diarioDaTurma(@PathVariable Long turmaId, Authentication auth) {
        professor(auth);
        Turma turma = turmas.findById(turmaId).orElseThrow(() -> naoEncontrado("Turma"));

        List<Aula> aulasTurma = aulas.findByTurmaIdOrderByDataAulaAsc(turmaId);
        List<Long> aulaIds = aulasTurma.stream().map(Aula::getId).toList();

        List<Usuario> alunos = matriculas.findByTurmaIdAndStatus(turmaId, StatusMatricula.ATIVA).stream()
                .map(Matricula::getUsuario)
                .filter(Objects::nonNull)
                .filter(u -> u.getTipo() == TipoUsuario.ALUNO)
                .sorted(Comparator.comparing(u -> u.getNome() != null ? u.getNome() : ""))
                .toList();

        List<Presenca> registros = aulaIds.isEmpty() ? List.of() : presencas.findByAulaIdIn(aulaIds);
        List<Avaliacao> notas = avaliacoes.findByTurmaId(turmaId);

        // presencas[aulaId][alunoId] = STATUS
        Map<Long, Map<Long, String>> chamada = new LinkedHashMap<>();
        for (Long aulaId : aulaIds) chamada.put(aulaId, new LinkedHashMap<>());
        for (Presenca p : registros) {
            if (p.getAula() == null || p.getUsuario() == null) continue;
            chamada.computeIfAbsent(p.getAula().getId(), k -> new LinkedHashMap<>())
                    .put(p.getUsuario().getId(), p.getStatus() != null ? p.getStatus().name() : "PRESENTE");
        }

        int totalAulasRealizadas = (int) aulasTurma.stream()
                .filter(a -> a.getStatus() == StatusAula.FINALIZADA || a.getStatus() == StatusAula.EM_ANDAMENTO)
                .count();

        List<Map<String, Object>> resumo = new ArrayList<>();
        for (Usuario aluno : alunos) {
            int presentes = 0, faltas = 0;
            for (Aula a : aulasTurma) {
                if (a.getStatus() != StatusAula.FINALIZADA && a.getStatus() != StatusAula.EM_ANDAMENTO) continue;
                String st = chamada.getOrDefault(a.getId(), Map.of()).get(aluno.getId());
                if (st == null || "AUSENTE".equals(st)) faltas++;
                else presentes++;
            }
            List<Avaliacao> doAluno = notas.stream()
                    .filter(n -> n.getAluno() != null && Objects.equals(n.getAluno().getId(), aluno.getId()))
                    .toList();

            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("alunoId", aluno.getId());
            linha.put("nome", aluno.getNome());
            linha.put("email", aluno.getEmail());
            linha.put("presencas", presentes);
            linha.put("faltas", faltas);
            linha.put("frequencia", totalAulasRealizadas == 0 ? 0
                    : Math.round((presentes * 1000.0) / totalAulasRealizadas) / 10.0);
            linha.put("totalAvaliacoes", doAluno.size());
            linha.put("media", media(doAluno));
            linha.put("situacao", doAluno.isEmpty() ? "SEM NOTAS" : (media(doAluno) >= 6 ? "APROVADO" : "EM RECUPERAÇÃO"));
            resumo.add(linha);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("turma", mapTurma(turma));
        out.put("aulas", aulasTurma.stream().map(this::mapAula).toList());
        out.put("alunos", alunos.stream().map(a -> Map.of("id", a.getId(), "nome", a.getNome() != null ? a.getNome() : "")).toList());
        out.put("chamada", chamada);
        out.put("avaliacoes", notas.stream().map(this::mapAvaliacao).toList());
        out.put("resumo", resumo);
        out.put("totalAulasRealizadas", totalAulasRealizadas);
        return out;
    }

    // ---------------------------------------------------------- registro da aula

    /** Início da aula pelo professor — abre o diário. */
    @PostMapping("/aula/{aulaId}/iniciar")
    public Map<String, Object> iniciar(@PathVariable Long aulaId, Authentication auth) {
        professor(auth);
        return mapAula(aulaService.iniciarAula(aulaId));
    }

    /** Fim da aula — consolida faltas e fecha o diário. */
    @PostMapping("/aula/{aulaId}/finalizar")
    public Map<String, Object> finalizar(@PathVariable Long aulaId, @RequestBody(required = false) Map<String, Object> body,
                                         Authentication auth) {
        professor(auth);
        if (body != null) {
            Aula aula = aulas.findById(aulaId).orElseThrow(() -> naoEncontrado("Aula"));
            if (body.containsKey("conteudoMinistrado")) aula.setConteudoMinistrado(str(body, "conteudoMinistrado"));
            if (body.containsKey("observacoes")) aula.setObservacoes(str(body, "observacoes"));
            aulas.save(aula);
        }
        return mapAula(aulaService.finalizarAula(aulaId));
    }

    /** Conteúdo ministrado e observações da aula. */
    @PutMapping("/aula/{aulaId}")
    public Map<String, Object> registrarConteudo(@PathVariable Long aulaId, @RequestBody Map<String, Object> body,
                                                 Authentication auth) {
        professor(auth);
        Aula aula = aulas.findById(aulaId).orElseThrow(() -> naoEncontrado("Aula"));
        if (body.containsKey("conteudoMinistrado")) aula.setConteudoMinistrado(str(body, "conteudoMinistrado"));
        if (body.containsKey("observacoes")) aula.setObservacoes(str(body, "observacoes"));
        if (body.containsKey("duracao")) aula.setDuracao(str(body, "duracao"));
        String iniciada = str(body, "iniciadaEm");
        if (iniciada != null) aula.setIniciadaEm(LocalDateTime.parse(iniciada.length() == 16 ? iniciada + ":00" : iniciada));
        String finalizada = str(body, "finalizadaEm");
        if (finalizada != null) aula.setFinalizadaEm(LocalDateTime.parse(finalizada.length() == 16 ? finalizada + ":00" : finalizada));
        return mapAula(aulas.save(aula));
    }

    // -------------------------------------------------------------- chamada

    @PostMapping("/aula/{aulaId}/presenca")
    public Map<String, Object> marcarPresenca(@PathVariable Long aulaId, @RequestBody Map<String, Object> body,
                                              Authentication auth) {
        professor(auth);
        Long alunoId = id(body, "alunoId");
        if (alunoId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alunoId é obrigatório");
        String status = str(body, "status");
        StatusPresenca sp = status != null ? StatusPresenca.valueOf(status.toUpperCase()) : StatusPresenca.PRESENTE;
        Presenca p = presencaService.definirStatus(alunoId, aulaId, sp, str(body, "observacao"));
        return Map.of("alunoId", alunoId, "aulaId", aulaId, "status", p.getStatus().name());
    }

    /** Chamada em lote: [{alunoId, status}] */
    @SuppressWarnings("unchecked")
    @PostMapping("/aula/{aulaId}/chamada")
    public Map<String, Object> chamada(@PathVariable Long aulaId, @RequestBody Map<String, Object> body,
                                       Authentication auth) {
        professor(auth);
        Object lista = body.get("registros");
        int total = 0;
        if (lista instanceof List<?> registros) {
            for (Object item : registros) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> reg = (Map<String, Object>) item;
                Long alunoId = id(reg, "alunoId");
                if (alunoId == null) continue;
                String status = str(reg, "status");
                presencaService.definirStatus(alunoId, aulaId,
                        status != null ? StatusPresenca.valueOf(status.toUpperCase()) : StatusPresenca.PRESENTE,
                        str(reg, "observacao"));
                total++;
            }
        }
        return Map.of("aulaId", aulaId, "registros", total);
    }

    // ----------------------------------------------------------- avaliações

    @PostMapping("/avaliacoes")
    public Map<String, Object> criarAvaliacao(@RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Long alunoId = id(body, "alunoId");
        Long turmaId = id(body, "turmaId");
        Double nota = num(body, "nota");
        if (alunoId == null || turmaId == null || nota == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "alunoId, turmaId e nota são obrigatórios");
        }
        Avaliacao a = new Avaliacao();
        a.setAluno(usuarios.findById(alunoId).orElseThrow(() -> naoEncontrado("Aluno")));
        a.setTurma(turmas.findById(turmaId).orElseThrow(() -> naoEncontrado("Turma")));
        Long aulaId = id(body, "aulaId");
        if (aulaId != null) a.setAula(aulas.findById(aulaId).orElseThrow(() -> naoEncontrado("Aula")));
        a.setTitulo(Objects.requireNonNullElse(str(body, "titulo"), "Avaliação"));
        a.setTipo(Objects.requireNonNullElse(str(body, "tipo"), "PROVA"));
        a.setNota(nota);
        a.setPeso(Objects.requireNonNullElse(num(body, "peso"), 1.0));
        return mapAvaliacao(avaliacoes.save(a));
    }

    @PutMapping("/avaliacoes/{id}")
    public Map<String, Object> atualizarAvaliacao(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                  Authentication auth) {
        professor(auth);
        Avaliacao a = avaliacoes.findById(id).orElseThrow(() -> naoEncontrado("Avaliação"));
        if (body.containsKey("titulo")) a.setTitulo(str(body, "titulo"));
        if (body.containsKey("tipo")) a.setTipo(str(body, "tipo"));
        if (body.containsKey("nota")) a.setNota(num(body, "nota"));
        if (body.containsKey("peso")) a.setPeso(num(body, "peso"));
        return mapAvaliacao(avaliacoes.save(a));
    }

    @DeleteMapping("/avaliacoes/{id}")
    public ResponseEntity<Void> excluirAvaliacao(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!avaliacoes.existsById(id)) throw naoEncontrado("Avaliação");
        avaliacoes.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------- aluno

    /** Boletim do próprio aluno: frequência, notas e média. */
    @GetMapping("/meu")
    public Map<String, Object> meuBoletim(Authentication auth) {
        Usuario user = autenticado(auth);
        List<Matricula> minhas = matriculas.findByUsuarioId(user.getId());
        List<Map<String, Object>> turmasResumo = new ArrayList<>();

        for (Matricula m : minhas) {
            if (m.getTurma() == null) continue;
            Long turmaId = m.getTurma().getId();
            List<Aula> aulasTurma = aulas.findByTurmaIdOrderByDataAulaAsc(turmaId).stream()
                    .filter(a -> a.getStatus() == StatusAula.FINALIZADA || a.getStatus() == StatusAula.EM_ANDAMENTO)
                    .toList();
            List<Long> ids = aulasTurma.stream().map(Aula::getId).toList();
            Set<Long> presentes = new HashSet<>();
            if (!ids.isEmpty()) {
                presencas.findByAulaIdIn(ids).stream()
                        .filter(p -> p.getUsuario() != null && Objects.equals(p.getUsuario().getId(), user.getId()))
                        .filter(p -> p.getStatus() != StatusPresenca.AUSENTE)
                        .forEach(p -> presentes.add(p.getAula().getId()));
            }
            List<Avaliacao> notas = avaliacoes.findByAlunoIdAndTurmaId(user.getId(), turmaId);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("turma", mapTurma(m.getTurma()));
            item.put("aulasRealizadas", aulasTurma.size());
            item.put("presencas", presentes.size());
            item.put("faltas", aulasTurma.size() - presentes.size());
            item.put("frequencia", aulasTurma.isEmpty() ? 0
                    : Math.round((presentes.size() * 1000.0) / aulasTurma.size()) / 10.0);
            item.put("avaliacoes", notas.stream().map(this::mapAvaliacao).toList());
            item.put("media", media(notas));
            turmasResumo.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("aluno", Map.of("id", user.getId(), "nome", user.getNome() != null ? user.getNome() : ""));
        out.put("turmas", turmasResumo);
        return out;
    }
}
