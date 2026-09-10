package com.devopsclassroom.service;

import com.devopsclassroom.dto.MensagemRequest;
import com.devopsclassroom.dto.MensagemResponse;
import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.AulaRepository;
import com.devopsclassroom.repository.MensagemRepository;
import com.devopsclassroom.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MensagemService {

    private final MensagemRepository mensagemRepository;
    private final AulaRepository aulaRepository;
    private final UsuarioRepository usuarioRepository;

    public MensagemService(MensagemRepository mensagemRepository, AulaRepository aulaRepository,
                          UsuarioRepository usuarioRepository) {
        this.mensagemRepository = mensagemRepository;
        this.aulaRepository = aulaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Mensagem criarMensagem(MensagemRequest request, Long usuarioId) {
        Aula aula = aulaRepository.findById(request.getAulaId())
                .orElseThrow(() -> new RuntimeException("Aula não encontrada"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Mensagem mensagem = new Mensagem();
        mensagem.setAula(aula);
        mensagem.setUsuario(usuario);
        mensagem.setConteudo(request.getConteudo());
        if (request.getTipo() != null) {
            mensagem.setTipo(TipoMensagem.valueOf(request.getTipo().toUpperCase()));
        }
        mensagem.setUrlMidia(request.getUrlMidia());
        mensagem.setNomeArquivo(request.getNomeArquivo());
        mensagem.setMimeType(request.getMimeType());
        if (request.getReplyToId() != null) {
            Mensagem origem = mensagemRepository.findById(request.getReplyToId())
                    .orElseThrow(() -> new RuntimeException("Mensagem de origem não encontrada"));
            if (!origem.getAula().getId().equals(aula.getId())) {
                throw new RuntimeException("A mensagem de origem pertence a outra aula");
            }
            mensagem.setReplyToId(origem.getId());
        }

        return mensagemRepository.save(mensagem);
    }

    public List<Mensagem> listarMensagensDaAula(Long aulaId) {
        return mensagemRepository.findByAulaIdOrderByCriadoEmAsc(aulaId);
    }

    /** Converte para DTO plano, resolvendo o autor e a mensagem respondida. */
    @Transactional(readOnly = true)
    public MensagemResponse montarResposta(Mensagem mensagem) {
        Mensagem origem = null;
        if (mensagem.getReplyToId() != null) {
            origem = mensagemRepository.findById(mensagem.getReplyToId()).orElse(null);
        }
        return MensagemResponse.fromEntity(mensagem, origem);
    }

    @Transactional(readOnly = true)
    public List<MensagemResponse> listarRespostasDaAula(Long aulaId) {
        List<Mensagem> mensagens = mensagemRepository.findByAulaIdOrderByCriadoEmAsc(aulaId);
        java.util.Map<Long, Mensagem> porId = new java.util.HashMap<>();
        mensagens.forEach(m -> porId.put(m.getId(), m));
        return mensagens.stream()
                .map(m -> MensagemResponse.fromEntity(m, m.getReplyToId() != null ? porId.get(m.getReplyToId()) : null))
                .toList();
    }
}
