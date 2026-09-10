package com.devopsclassroom.service;

import com.devopsclassroom.dto.AulaRequest;
import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AulaService {

    private final AulaRepository aulaRepository;
    private final CursoRepository cursoRepository;
    private final TurmaRepository turmaRepository;
    private final MatriculaRepository matriculaRepository;
    private final PresencaRepository presencaRepository;

    public AulaService(AulaRepository aulaRepository, CursoRepository cursoRepository,
                       TurmaRepository turmaRepository, MatriculaRepository matriculaRepository,
                       PresencaRepository presencaRepository) {
        this.aulaRepository = aulaRepository;
        this.cursoRepository = cursoRepository;
        this.turmaRepository = turmaRepository;
        this.matriculaRepository = matriculaRepository;
        this.presencaRepository = presencaRepository;
    }

    @Transactional
    public Aula criarAula(AulaRequest request) {
        Curso curso = cursoRepository.findById(request.getCursoId())
                .orElseThrow(() -> new RuntimeException("Curso não encontrado"));

        Aula aula = new Aula();
        aula.setCurso(curso);
        if (request.getTurmaId() != null) {
            aula.setTurma(turmaRepository.findById(request.getTurmaId())
                    .orElseThrow(() -> new RuntimeException("Turma não encontrada")));
        }
        aula.setTitulo(request.getTitulo());
        aula.setDescricao(request.getDescricao());
        if (request.getDataAula() != null) {
            String data = request.getDataAula();
            aula.setDataAula(LocalDateTime.parse(data.length() == 16 ? data + ":00" : data));
        }
        aula.setDuracao(request.getDuracao());
        if (request.getStatus() != null) {
            aula.setStatus(StatusAula.valueOf(request.getStatus().toUpperCase()));
        }

        return aulaRepository.save(aula);
    }

    public List<Aula> listarAulasPorCurso(Long cursoId) {
        return aulaRepository.findByCursoId(cursoId);
    }

    public List<Aula> listarAulasPorTurma(Long turmaId) {
        return aulaRepository.findByTurmaIdOrderByDataAulaAsc(turmaId);
    }

    public List<Aula> listarTodas() {
        return aulaRepository.findAll();
    }

    public Aula buscarAula(Long id) {
        return aulaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Aula não encontrada"));
    }

    /** Inicia a aula: libera o chat, a chamada e o preenchimento do diário. */
    @Transactional
    public Aula iniciarAula(Long aulaId) {
        Aula aula = buscarAula(aulaId);
        aula.setStatus(StatusAula.EM_ANDAMENTO);
        if (aula.getIniciadaEm() == null) aula.setIniciadaEm(LocalDateTime.now());
        aula.setFinalizadaEm(null);
        return aulaRepository.save(aula);
    }

    /**
     * Finaliza a aula, fecha o diário e consolida a chamada: todo aluno
     * matriculado na turma que não registrou presença é marcado como AUSENTE.
     */
    @Transactional
    public Aula finalizarAula(Long aulaId) {
        Aula aula = buscarAula(aulaId);
        aula.setStatus(StatusAula.FINALIZADA);
        aula.setFinalizadaEm(LocalDateTime.now());
        if (aula.getIniciadaEm() == null) aula.setIniciadaEm(aula.getDataAula() != null ? aula.getDataAula() : LocalDateTime.now());
        consolidarFaltas(aula);
        return aulaRepository.save(aula);
    }

    private void consolidarFaltas(Aula aula) {
        if (aula.getTurma() == null) return;
        Set<Long> jaRegistrados = presencaRepository.findByAulaId(aula.getId()).stream()
                .map(p -> p.getUsuario().getId())
                .collect(Collectors.toSet());

        List<Matricula> matriculas = matriculaRepository.findByTurmaIdAndStatus(
                aula.getTurma().getId(), StatusMatricula.ATIVA);

        for (Matricula matricula : matriculas) {
            Usuario aluno = matricula.getUsuario();
            if (aluno == null || jaRegistrados.contains(aluno.getId())) continue;
            if (aluno.getTipo() != TipoUsuario.ALUNO) continue;
            Presenca falta = new Presenca();
            falta.setAula(aula);
            falta.setUsuario(aluno);
            falta.setStatus(StatusPresenca.AUSENTE);
            presencaRepository.save(falta);
        }
    }

    public List<Aula> listarAulasEmAndamento() {
        return aulaRepository.findByStatus(StatusAula.EM_ANDAMENTO);
    }
}
