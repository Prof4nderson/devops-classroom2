package com.devopsclassroom.service;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.AulaRepository;
import com.devopsclassroom.repository.PresencaRepository;
import com.devopsclassroom.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PresencaService {

    private final PresencaRepository presencaRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioRepository usuarioRepository;

    public PresencaService(PresencaRepository presencaRepository, AulaRepository aulaRepository,
                          UsuarioRepository usuarioRepository) {
        this.presencaRepository = presencaRepository;
        this.aulaRepository = aulaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Presenca registrarPresenca(Long usuarioId, Long aulaId) {
        return definirStatus(usuarioId, aulaId, StatusPresenca.PRESENTE, null);
    }

    /** Cria ou atualiza o registro de presença do aluno na aula. */
    @Transactional
    public Presenca definirStatus(Long usuarioId, Long aulaId, StatusPresenca status, String observacao) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new RuntimeException("Aula não encontrada"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Presenca presenca = presencaRepository.findByUsuarioIdAndAulaId(usuarioId, aulaId)
                .orElseGet(() -> {
                    Presenca nova = new Presenca();
                    nova.setUsuario(usuario);
                    nova.setAula(aula);
                    return nova;
                });
        presenca.setStatus(status != null ? status : StatusPresenca.PRESENTE);
        if (observacao != null) presenca.setObservacao(observacao);
        return presencaRepository.save(presenca);
    }

    public List<Usuario> listarPresentes(Long aulaId) {
        return presencaRepository.findByAulaId(aulaId).stream()
                .filter(p -> p.getStatus() != StatusPresenca.AUSENTE)
                .map(Presenca::getUsuario)
                .toList();
    }

    public List<Presenca> listarRegistros(Long aulaId) {
        return presencaRepository.findByAulaId(aulaId);
    }

    public long contarPresentes(Long aulaId) {
        return listarPresentes(aulaId).size();
    }

    public boolean verificarPresenca(Long usuarioId, Long aulaId) {
        return presencaRepository.findByUsuarioIdAndAulaId(usuarioId, aulaId)
                .map(p -> p.getStatus() != StatusPresenca.AUSENTE)
                .orElse(false);
    }
}
