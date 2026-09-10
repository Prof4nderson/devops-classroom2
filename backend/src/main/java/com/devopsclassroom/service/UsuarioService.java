package com.devopsclassroom.service;

import com.devopsclassroom.config.JwtConfig;
import com.devopsclassroom.dto.*;
import com.devopsclassroom.entity.Usuario;
import com.devopsclassroom.entity.TipoUsuario;
import com.devopsclassroom.entity.Curso;
import com.devopsclassroom.entity.Instituicao;
import com.devopsclassroom.entity.Matricula;
import com.devopsclassroom.entity.StatusMatricula;
import com.devopsclassroom.entity.Turma;
import com.devopsclassroom.repository.CursoRepository;
import com.devopsclassroom.repository.InstituicaoRepository;
import com.devopsclassroom.repository.MatriculaRepository;
import com.devopsclassroom.repository.TurmaRepository;
import com.devopsclassroom.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final InstituicaoRepository instituicaoRepository;
    private final CursoRepository cursoRepository;
    private final TurmaRepository turmaRepository;
    private final MatriculaRepository matriculaRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtConfig jwtConfig,
                          InstituicaoRepository instituicaoRepository, CursoRepository cursoRepository,
                          TurmaRepository turmaRepository, MatriculaRepository matriculaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.instituicaoRepository = instituicaoRepository;
        this.cursoRepository = cursoRepository;
        this.turmaRepository = turmaRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional
    public AuthResponse autenticar(AuthRequest request) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByLogin(request.getLogin());
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }
        String token = jwtConfig.generateToken(usuario.getLogin(), usuario.getId(), usuario.getTipo().name());
        return new AuthResponse(token, usuario.getId(), usuario.getNome(), usuario.getLogin(), usuario.getTipo().name());
    }

    @Transactional
    public UsuarioResponse criarUsuario(UsuarioRequest request) {
        if (usuarioRepository.existsByLogin(request.getLogin())) {
            throw new RuntimeException("Login já existe");
        }

        Usuario usuario = new Usuario();
        usuario.setLogin(request.getLogin());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        // Cadastro público é exclusivamente de alunos. Professores devem ser criados por fluxo administrativo protegido.
        usuario.setTipo(TipoUsuario.ALUNO);
        usuario.setBio(request.getBio());

        // O aluno precisa escolher instituição, curso e turma já cadastrados.
        if (request.getInstituicaoId() == null || request.getCursoId() == null || request.getTurmaId() == null) {
            throw new RuntimeException("Selecione instituição, curso e turma para concluir o cadastro");
        }
        Instituicao instituicao = instituicaoRepository.findById(request.getInstituicaoId())
                .orElseThrow(() -> new RuntimeException("Instituição não encontrada"));
        Curso curso = cursoRepository.findById(request.getCursoId())
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));
        Turma turma = turmaRepository.findById(request.getTurmaId())
                .orElseThrow(() -> new RuntimeException("Turma não encontrada"));
        if (curso.getInstituicao() == null || !curso.getInstituicao().getId().equals(instituicao.getId())) {
            throw new RuntimeException("O curso selecionado não pertence à instituição informada");
        }
        if (turma.getCurso() == null || !turma.getCurso().getId().equals(curso.getId())) {
            throw new RuntimeException("A turma selecionada não pertence ao curso informado");
        }
        usuario.setInstituicao(instituicao.getNome());

        usuario = usuarioRepository.save(usuario);

        Matricula matricula = new Matricula();
        matricula.setUsuario(usuario);
        matricula.setCurso(curso);
        matricula.setTurma(turma);
        matricula.setStatus(StatusMatricula.ATIVA);
        matriculaRepository.save(matricula);

        return UsuarioResponse.fromEntity(usuario);
    }

    @Transactional
    public UsuarioResponse criarUsuarioAdministrativo(UsuarioRequest request) {
        if (usuarioRepository.existsByLogin(request.getLogin())) throw new RuntimeException("Login já existe");
        Usuario usuario = new Usuario();
        usuario.setLogin(request.getLogin());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        usuario.setTipo(TipoUsuario.valueOf(request.getTipo().toUpperCase()));
        usuario.setInstituicao(request.getInstituicao());
        usuario.setBio(request.getBio());
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    @Transactional
    public void excluirUsuario(Long id) {
        if (!usuarioRepository.existsById(id)) throw new RuntimeException("Usuário não encontrado");
        usuarioRepository.deleteById(id);
    }

    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    public UsuarioResponse buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::fromEntity)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @Transactional
    public UsuarioResponse atualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (request.getNome() != null) usuario.setNome(request.getNome());
        if (request.getEmail() != null) usuario.setEmail(request.getEmail());
        if (request.getTelefone() != null) usuario.setTelefone(request.getTelefone());
        if (request.getInstituicao() != null) usuario.setInstituicao(request.getInstituicao());
        if (request.getBio() != null) usuario.setBio(request.getBio());
        if (request.getSenha() != null) usuario.setSenha(passwordEncoder.encode(request.getSenha()));

        usuario = usuarioRepository.save(usuario);
        return UsuarioResponse.fromEntity(usuario);
    }

    public List<UsuarioResponse> buscarAlunos(String nome) {
        return usuarioRepository.findByNomeContainingIgnoreCase(nome).stream()
                .filter(u -> u.getTipo() == TipoUsuario.ALUNO)
                .map(UsuarioResponse::fromEntity)
                .toList();
    }

    public List<UsuarioResponse> buscarProfessores() {
        return usuarioRepository.findByTipo(TipoUsuario.PROFESSOR).stream()
                .map(UsuarioResponse::fromEntity)
                .toList();
    }
}
