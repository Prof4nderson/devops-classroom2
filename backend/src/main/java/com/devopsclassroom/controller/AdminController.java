package com.devopsclassroom.controller;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CRUD administrativo (somente PROFESSOR/ADMIN) das tabelas do sistema:
 * usuários, instituições, períodos, cursos, turmas, matrículas, trilhas e aulas.
 * Todas as respostas são mapas planos para evitar problemas de lazy loading.
 */
@RestController
@RequestMapping("/api/admin")
@Transactional
public class AdminController {

    private final UsuarioRepository usuarios;
    private final InstituicaoRepository instituicoes;
    private final PeriodoRepository periodos;
    private final CursoRepository cursos;
    private final TurmaRepository turmas;
    private final MatriculaRepository matriculas;
    private final TrilhaRepository trilhas;
    private final AulaRepository aulas;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UsuarioRepository usuarios, InstituicaoRepository instituicoes, PeriodoRepository periodos,
                           CursoRepository cursos, TurmaRepository turmas, MatriculaRepository matriculas,
                           TrilhaRepository trilhas, AulaRepository aulas, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.instituicoes = instituicoes;
        this.periodos = periodos;
        this.cursos = cursos;
        this.turmas = turmas;
        this.matriculas = matriculas;
        this.trilhas = trilhas;
        this.aulas = aulas;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------------- helpers

    private Usuario professor(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Usuario user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }
        if (user.getTipo() != TipoUsuario.PROFESSOR && user.getTipo() != TipoUsuario.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso permitido somente ao professor");
        }
        return user;
    }

    private static String str(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static Long id(Map<String, Object> body, String key) {
        String value = str(body, key);
        if (value == null) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static <T> T required(Optional<T> value, String mensagem) {
        return value.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem));
    }

    private static ResponseStatusException naoEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado");
    }

    // ------------------------------------------------------------ mapeadores

    private Map<String, Object> mapUsuario(Usuario u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("login", u.getLogin());
        m.put("nome", u.getNome());
        m.put("email", u.getEmail());
        m.put("telefone", u.getTelefone());
        m.put("tipo", u.getTipo() != null ? u.getTipo().name() : null);
        m.put("instituicao", u.getInstituicao());
        m.put("bio", u.getBio());
        return m;
    }

    private Map<String, Object> mapInstituicao(Instituicao i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("nome", i.getNome());
        m.put("codigo", i.getCodigo());
        m.put("descricao", i.getDescricao());
        return m;
    }

    private Map<String, Object> mapPeriodo(Periodo p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("nome", p.getNome());
        m.put("codigo", p.getCodigo());
        m.put("instituicaoId", p.getInstituicao() != null ? p.getInstituicao().getId() : null);
        m.put("instituicao", p.getInstituicao() != null ? p.getInstituicao().getNome() : null);
        return m;
    }

    private Map<String, Object> mapCurso(Curso c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("nome", c.getNome());
        m.put("codigo", c.getCodigo());
        m.put("descricao", c.getDescricao());
        m.put("instituicaoId", c.getInstituicao() != null ? c.getInstituicao().getId() : null);
        m.put("instituicao", c.getInstituicao() != null ? c.getInstituicao().getNome() : null);
        m.put("professorId", c.getProfessor() != null ? c.getProfessor().getId() : null);
        m.put("professor", c.getProfessor() != null ? c.getProfessor().getNome() : null);
        return m;
    }

    private Map<String, Object> mapTurma(Turma t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("codigo", t.getCodigo());
        m.put("nome", t.getNome());
        m.put("cursoId", t.getCurso() != null ? t.getCurso().getId() : null);
        m.put("curso", t.getCurso() != null ? t.getCurso().getNome() : null);
        m.put("periodoId", t.getPeriodo() != null ? t.getPeriodo().getId() : null);
        m.put("periodo", t.getPeriodo() != null ? t.getPeriodo().getNome() : null);
        m.put("professorId", t.getProfessor() != null ? t.getProfessor().getId() : null);
        m.put("professor", t.getProfessor() != null ? t.getProfessor().getNome() : null);
        return m;
    }

    private Map<String, Object> mapMatricula(Matricula m0) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", m0.getId());
        m.put("usuarioId", m0.getUsuario() != null ? m0.getUsuario().getId() : null);
        m.put("usuario", m0.getUsuario() != null ? m0.getUsuario().getNome() : null);
        m.put("cursoId", m0.getCurso() != null ? m0.getCurso().getId() : null);
        m.put("curso", m0.getCurso() != null ? m0.getCurso().getNome() : null);
        m.put("turmaId", m0.getTurma() != null ? m0.getTurma().getId() : null);
        m.put("turma", m0.getTurma() != null ? m0.getTurma().getCodigo() : null);
        m.put("status", m0.getStatus() != null ? m0.getStatus().name() : null);
        return m;
    }

    private Map<String, Object> mapTrilha(Trilha t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("titulo", t.getTitulo());
        m.put("descricao", t.getDescricao());
        m.put("conteudoJson", t.getConteudoJson());
        m.put("ativa", t.isAtiva());
        return m;
    }

    private Map<String, Object> mapAula(Aula a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("titulo", a.getTitulo());
        m.put("descricao", a.getDescricao());
        m.put("duracao", a.getDuracao());
        m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
        m.put("dataAula", a.getDataAula() != null ? a.getDataAula().toString() : null);
        m.put("cursoId", a.getCurso() != null ? a.getCurso().getId() : null);
        m.put("curso", a.getCurso() != null ? a.getCurso().getNome() : null);
        m.put("turmaId", a.getTurma() != null ? a.getTurma().getId() : null);
        m.put("turma", a.getTurma() != null ? a.getTurma().getCodigo() : null);
        return m;
    }

    // -------------------------------------------------------------- usuários

    @GetMapping("/usuarios")
    public List<Map<String, Object>> listarUsuarios(Authentication auth) {
        professor(auth);
        return usuarios.findAll().stream().map(this::mapUsuario).toList();
    }

    @PostMapping("/usuarios")
    public Map<String, Object> criarUsuario(@RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Usuario u = new Usuario();
        String login = str(body, "login");
        if (login == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login é obrigatório");
        if (usuarios.existsByLogin(login)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Login já cadastrado");
        u.setLogin(login);
        u.setNome(Objects.requireNonNullElse(str(body, "nome"), login));
        u.setEmail(str(body, "email"));
        u.setTelefone(str(body, "telefone"));
        u.setInstituicao(str(body, "instituicao"));
        u.setBio(str(body, "bio"));
        u.setTipo(TipoUsuario.fromValue(Objects.requireNonNullElse(str(body, "tipo"), "ALUNO")));
        String senha = str(body, "senha");
        u.setSenha(passwordEncoder.encode(senha != null ? senha : "123456"));
        return mapUsuario(usuarios.save(u));
    }

    @PutMapping("/usuarios/{id}")
    public Map<String, Object> atualizarUsuario(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Usuario u = usuarios.findById(id).orElseThrow(AdminController::naoEncontrado);
        if (str(body, "nome") != null) u.setNome(str(body, "nome"));
        if (body.containsKey("email")) u.setEmail(str(body, "email"));
        if (body.containsKey("telefone")) u.setTelefone(str(body, "telefone"));
        if (body.containsKey("instituicao")) u.setInstituicao(str(body, "instituicao"));
        if (body.containsKey("bio")) u.setBio(str(body, "bio"));
        if (str(body, "tipo") != null) u.setTipo(TipoUsuario.fromValue(str(body, "tipo")));
        if (str(body, "senha") != null) u.setSenha(passwordEncoder.encode(str(body, "senha")));
        return mapUsuario(usuarios.save(u));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> excluirUsuario(@PathVariable Long id, Authentication auth) {
        Usuario atual = professor(auth);
        if (atual.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível excluir o próprio usuário");
        }
        if (!usuarios.existsById(id)) throw naoEncontrado();
        matriculas.findByUsuarioId(id).forEach(matriculas::delete);
        usuarios.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --------------------------------------------------------- instituições

    @GetMapping("/instituicoes")
    public List<Map<String, Object>> listarInstituicoes(Authentication auth) {
        professor(auth);
        return instituicoes.findAll().stream().map(this::mapInstituicao).toList();
    }

    @PostMapping("/instituicoes")
    public Map<String, Object> criarInstituicao(@RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Instituicao i = new Instituicao();
        i.setNome(str(body, "nome"));
        i.setCodigo(str(body, "codigo"));
        i.setDescricao(str(body, "descricao"));
        if (i.getNome() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome é obrigatório");
        return mapInstituicao(instituicoes.save(i));
    }

    @PutMapping("/instituicoes/{id}")
    public Map<String, Object> atualizarInstituicao(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Instituicao i = instituicoes.findById(id).orElseThrow(AdminController::naoEncontrado);
        if (str(body, "nome") != null) i.setNome(str(body, "nome"));
        if (body.containsKey("codigo")) i.setCodigo(str(body, "codigo"));
        if (body.containsKey("descricao")) i.setDescricao(str(body, "descricao"));
        return mapInstituicao(instituicoes.save(i));
    }

    @DeleteMapping("/instituicoes/{id}")
    public ResponseEntity<Void> excluirInstituicao(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!instituicoes.existsById(id)) throw naoEncontrado();
        instituicoes.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------- períodos

    @GetMapping("/periodos")
    public List<Map<String, Object>> listarPeriodos(Authentication auth) {
        professor(auth);
        return periodos.findAll().stream().map(this::mapPeriodo).toList();
    }

    @PostMapping("/periodos")
    public Map<String, Object> criarPeriodo(@RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Periodo p = new Periodo();
        p.setNome(str(body, "nome"));
        p.setCodigo(str(body, "codigo"));
        p.setInstituicao(required(instituicoes.findById(Objects.requireNonNullElse(id(body, "instituicaoId"), -1L)),
                "Instituição é obrigatória"));
        if (p.getNome() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome é obrigatório");
        return mapPeriodo(periodos.save(p));
    }

    @PutMapping("/periodos/{id}")
    public Map<String, Object> atualizarPeriodo(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Periodo p = periodos.findById(id).orElseThrow(AdminController::naoEncontrado);
        if (str(body, "nome") != null) p.setNome(str(body, "nome"));
        if (body.containsKey("codigo")) p.setCodigo(str(body, "codigo"));
        Long instId = id(body, "instituicaoId");
        if (instId != null) p.setInstituicao(required(instituicoes.findById(instId), "Instituição inválida"));
        return mapPeriodo(periodos.save(p));
    }

    @DeleteMapping("/periodos/{id}")
    public ResponseEntity<Void> excluirPeriodo(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!periodos.existsById(id)) throw naoEncontrado();
        periodos.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- cursos

    @GetMapping("/cursos")
    public List<Map<String, Object>> listarCursos(Authentication auth) {
        professor(auth);
        return cursos.findAll().stream().map(this::mapCurso).toList();
    }

    @PostMapping("/cursos")
    public Map<String, Object> criarCurso(@RequestBody Map<String, Object> body, Authentication auth) {
        Usuario atual = professor(auth);
        Curso c = new Curso();
        c.setNome(str(body, "nome"));
        c.setCodigo(str(body, "codigo"));
        c.setDescricao(str(body, "descricao"));
        Long instId = id(body, "instituicaoId");
        if (instId != null) c.setInstituicao(required(instituicoes.findById(instId), "Instituição inválida"));
        Long profId = id(body, "professorId");
        c.setProfessor(profId != null ? required(usuarios.findById(profId), "Professor inválido") : atual);
        if (c.getNome() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome é obrigatório");
        return mapCurso(cursos.save(c));
    }

    @PutMapping("/cursos/{id}")
    public Map<String, Object> atualizarCurso(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Curso c = cursos.findById(id).orElseThrow(AdminController::naoEncontrado);
        if (str(body, "nome") != null) c.setNome(str(body, "nome"));
        if (body.containsKey("codigo")) c.setCodigo(str(body, "codigo"));
        if (body.containsKey("descricao")) c.setDescricao(str(body, "descricao"));
        Long instId = id(body, "instituicaoId");
        if (instId != null) c.setInstituicao(required(instituicoes.findById(instId), "Instituição inválida"));
        Long profId = id(body, "professorId");
        if (profId != null) c.setProfessor(required(usuarios.findById(profId), "Professor inválido"));
        return mapCurso(cursos.save(c));
    }

    @DeleteMapping("/cursos/{id}")
    public ResponseEntity<Void> excluirCurso(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!cursos.existsById(id)) throw naoEncontrado();
        cursos.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- turmas

    @GetMapping("/turmas")
    public List<Map<String, Object>> listarTurmas(Authentication auth) {
        professor(auth);
        return turmas.findAll().stream().map(this::mapTurma).toList();
    }

    @PostMapping("/turmas")
    public Map<String, Object> criarTurma(@RequestBody Map<String, Object> body, Authentication auth) {
        Usuario atual = professor(auth);
        Turma t = new Turma();
        t.setCodigo(str(body, "codigo"));
        t.setNome(Objects.requireNonNullElse(str(body, "nome"), Objects.requireNonNullElse(t.getCodigo(), "Turma")));
        t.setCurso(required(cursos.findById(Objects.requireNonNullElse(id(body, "cursoId"), -1L)), "Curso é obrigatório"));
        t.setPeriodo(required(periodos.findById(Objects.requireNonNullElse(id(body, "periodoId"), -1L)), "Período é obrigatório"));
        Long profId = id(body, "professorId");
        t.setProfessor(profId != null ? required(usuarios.findById(profId), "Professor inválido") : atual);
        if (t.getCodigo() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código é obrigatório");
        return mapTurma(turmas.save(t));
    }

    @PutMapping("/turmas/{id}")
    public Map<String, Object> atualizarTurma(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Turma t = turmas.findById(id).orElseThrow(AdminController::naoEncontrado);
        if (str(body, "codigo") != null) t.setCodigo(str(body, "codigo"));
        if (body.containsKey("nome")) t.setNome(str(body, "nome"));
        Long cursoId = id(body, "cursoId");
        if (cursoId != null) t.setCurso(required(cursos.findById(cursoId), "Curso inválido"));
        Long periodoId = id(body, "periodoId");
        if (periodoId != null) t.setPeriodo(required(periodos.findById(periodoId), "Período inválido"));
        Long profId = id(body, "professorId");
        if (profId != null) t.setProfessor(required(usuarios.findById(profId), "Professor inválido"));
        return mapTurma(turmas.save(t));
    }

    @DeleteMapping("/turmas/{id}")
    public ResponseEntity<Void> excluirTurma(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!turmas.existsById(id)) throw naoEncontrado();
        turmas.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ matrículas

    @GetMapping("/matriculas")
    public List<Map<String, Object>> listarMatriculas(Authentication auth) {
        professor(auth);
        return matriculas.findAll().stream().map(this::mapMatricula).toList();
    }

    @PostMapping("/matriculas")
    public Map<String, Object> criarMatricula(@RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Matricula m = new Matricula();
        m.setUsuario(required(usuarios.findById(Objects.requireNonNullElse(id(body, "usuarioId"), -1L)), "Aluno é obrigatório"));
        m.setCurso(required(cursos.findById(Objects.requireNonNullElse(id(body, "cursoId"), -1L)), "Curso é obrigatório"));
        Long turmaId = id(body, "turmaId");
        if (turmaId != null) m.setTurma(required(turmas.findById(turmaId), "Turma inválida"));
        String status = str(body, "status");
        m.setStatus(status != null ? StatusMatricula.valueOf(status.toUpperCase()) : StatusMatricula.ATIVA);
        return mapMatricula(matriculas.save(m));
    }

    @PutMapping("/matriculas/{id}")
    public Map<String, Object> atualizarMatricula(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Matricula m = matriculas.findById(id).orElseThrow(AdminController::naoEncontrado);
        Long usuarioId = id(body, "usuarioId");
        if (usuarioId != null) m.setUsuario(required(usuarios.findById(usuarioId), "Aluno inválido"));
        Long cursoId = id(body, "cursoId");
        if (cursoId != null) m.setCurso(required(cursos.findById(cursoId), "Curso inválido"));
        Long turmaId = id(body, "turmaId");
        if (turmaId != null) m.setTurma(required(turmas.findById(turmaId), "Turma inválida"));
        String status = str(body, "status");
        if (status != null) m.setStatus(StatusMatricula.valueOf(status.toUpperCase()));
        return mapMatricula(matriculas.save(m));
    }

    @DeleteMapping("/matriculas/{id}")
    public ResponseEntity<Void> excluirMatricula(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!matriculas.existsById(id)) throw naoEncontrado();
        matriculas.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------- trilhas

    @GetMapping("/trilhas")
    public List<Map<String, Object>> listarTrilhas(Authentication auth) {
        professor(auth);
        return trilhas.findAll().stream().map(this::mapTrilha).toList();
    }

    @PostMapping("/trilhas")
    public Map<String, Object> criarTrilha(@RequestBody Map<String, Object> body, Authentication auth) {
        Usuario atual = professor(auth);
        Trilha t = new Trilha();
        t.setTitulo(str(body, "titulo"));
        t.setDescricao(str(body, "descricao"));
        t.setConteudoJson(Objects.requireNonNullElse(str(body, "conteudoJson"), "[]"));
        t.setAtiva(!Boolean.FALSE.equals(body.get("ativa")));
        t.setProfessor(atual);
        if (t.getTitulo() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título é obrigatório");
        return mapTrilha(trilhas.save(t));
    }

    @PutMapping("/trilhas/{id}")
    public Map<String, Object> atualizarTrilha(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Trilha t = trilhas.findById(id).orElseThrow(AdminController::naoEncontrado);
        if (str(body, "titulo") != null) t.setTitulo(str(body, "titulo"));
        if (body.containsKey("descricao")) t.setDescricao(str(body, "descricao"));
        if (str(body, "conteudoJson") != null) t.setConteudoJson(str(body, "conteudoJson"));
        if (body.containsKey("ativa")) t.setAtiva(!Boolean.FALSE.equals(body.get("ativa")));
        return mapTrilha(trilhas.save(t));
    }

    @DeleteMapping("/trilhas/{id}")
    public ResponseEntity<Void> excluirTrilha(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!trilhas.existsById(id)) throw naoEncontrado();
        trilhas.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------- aulas

    @GetMapping("/aulas")
    public List<Map<String, Object>> listarAulas(Authentication auth) {
        professor(auth);
        return aulas.findAll().stream().map(this::mapAula).toList();
    }

    @PostMapping("/aulas")
    public Map<String, Object> criarAula(@RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Aula a = new Aula();
        a.setTitulo(str(body, "titulo"));
        a.setDescricao(str(body, "descricao"));
        a.setDuracao(str(body, "duracao"));
        a.setCurso(required(cursos.findById(Objects.requireNonNullElse(id(body, "cursoId"), -1L)), "Curso é obrigatório"));
        Long turmaId = id(body, "turmaId");
        if (turmaId != null) a.setTurma(required(turmas.findById(turmaId), "Turma inválida"));
        String data = str(body, "dataAula");
        a.setDataAula(data != null ? LocalDateTime.parse(data.length() == 16 ? data + ":00" : data) : LocalDateTime.now());
        String status = str(body, "status");
        a.setStatus(status != null ? StatusAula.valueOf(status.toUpperCase()) : StatusAula.AGENDADA);
        if (a.getTitulo() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título é obrigatório");
        return mapAula(aulas.save(a));
    }

    @PutMapping("/aulas/{id}")
    public Map<String, Object> atualizarAula(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        professor(auth);
        Aula a = aulas.findById(id).orElseThrow(AdminController::naoEncontrado);
        if (str(body, "titulo") != null) a.setTitulo(str(body, "titulo"));
        if (body.containsKey("descricao")) a.setDescricao(str(body, "descricao"));
        if (body.containsKey("duracao")) a.setDuracao(str(body, "duracao"));
        Long cursoId = id(body, "cursoId");
        if (cursoId != null) a.setCurso(required(cursos.findById(cursoId), "Curso inválido"));
        Long turmaId = id(body, "turmaId");
        if (turmaId != null) a.setTurma(required(turmas.findById(turmaId), "Turma inválida"));
        String data = str(body, "dataAula");
        if (data != null) a.setDataAula(LocalDateTime.parse(data.length() == 16 ? data + ":00" : data));
        String status = str(body, "status");
        if (status != null) a.setStatus(StatusAula.valueOf(status.toUpperCase()));
        return mapAula(aulas.save(a));
    }

    @DeleteMapping("/aulas/{id}")
    public ResponseEntity<Void> excluirAula(@PathVariable Long id, Authentication auth) {
        professor(auth);
        if (!aulas.existsById(id)) throw naoEncontrado();
        aulas.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
