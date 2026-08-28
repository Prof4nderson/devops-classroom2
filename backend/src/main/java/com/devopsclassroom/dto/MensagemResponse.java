package com.devopsclassroom.dto;

import com.devopsclassroom.entity.Mensagem;

import java.time.LocalDateTime;

/**
 * Payload plano do chat. Evita enviar a entidade Mensagem (proxies lazy) e
 * garante que o nome de quem escreveu — e de quem está sendo respondido —
 * chegue ao frontend.
 */
public class MensagemResponse {

    private Long id;
    private String conteudo;
    private String tipo;
    private String urlMidia;
    private String nomeArquivo;
    private String mimeType;
    private Long usuarioId;
    private String usuarioNome;
    private String usuarioTipo;
    private LocalDateTime criadoEm;
    private Long replyToId;
    private String replyToNome;
    private String replyToConteudo;

    public static MensagemResponse fromEntity(Mensagem mensagem, Mensagem origem) {
        MensagemResponse dto = new MensagemResponse();
        dto.id = mensagem.getId();
        dto.conteudo = mensagem.getConteudo();
        dto.tipo = mensagem.getTipo() != null ? mensagem.getTipo().name() : "TEXT";
        dto.urlMidia = mensagem.getUrlMidia();
        dto.nomeArquivo = mensagem.getNomeArquivo();
        dto.mimeType = mensagem.getMimeType();
        dto.criadoEm = mensagem.getCriadoEm();
        dto.replyToId = mensagem.getReplyToId();

        if (mensagem.getUsuario() != null) {
            dto.usuarioId = mensagem.getUsuario().getId();
            dto.usuarioNome = mensagem.getUsuario().getNome() != null
                    ? mensagem.getUsuario().getNome()
                    : mensagem.getUsuario().getLogin();
            dto.usuarioTipo = mensagem.getUsuario().getTipo() != null
                    ? mensagem.getUsuario().getTipo().name()
                    : null;
        }

        if (origem != null) {
            dto.replyToId = origem.getId();
            if (origem.getUsuario() != null) {
                dto.replyToNome = origem.getUsuario().getNome() != null
                        ? origem.getUsuario().getNome()
                        : origem.getUsuario().getLogin();
            }
            dto.replyToConteudo = origem.getConteudo();
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getConteudo() { return conteudo; }
    public String getTipo() { return tipo; }
    public String getUrlMidia() { return urlMidia; }
    public String getNomeArquivo() { return nomeArquivo; }
    public String getMimeType() { return mimeType; }
    public Long getUsuarioId() { return usuarioId; }
    public String getUsuarioNome() { return usuarioNome; }
    public String getUsuarioTipo() { return usuarioTipo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Long getReplyToId() { return replyToId; }
    public String getReplyToNome() { return replyToNome; }
    public String getReplyToConteudo() { return replyToConteudo; }
}
