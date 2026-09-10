package com.devopsclassroom.controller;

import com.devopsclassroom.entity.*;
import com.devopsclassroom.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aprendizado")
public class LearningController {
    private final TrilhaRepository trilhas;
    private final TrabalhoRepository trabalhos;
    private final EntregaTrabalhoRepository entregas;
    private final EquipeRepository equipes;
    private final TurmaRepository turmas;
    private final UsuarioRepository usuarios;
    public LearningController(TrilhaRepository trilhas, TrabalhoRepository trabalhos, EntregaTrabalhoRepository entregas,
                              EquipeRepository equipes, TurmaRepository turmas, UsuarioRepository usuarios) {
        this.trilhas=trilhas; this.trabalhos=trabalhos; this.entregas=entregas; this.equipes=equipes; this.turmas=turmas; this.usuarios=usuarios;
    }
    private Usuario user(Authentication auth) { return (Usuario) auth.getPrincipal(); }
    private void professor(Authentication auth) { if (user(auth).getTipo()!=TipoUsuario.PROFESSOR && user(auth).getTipo()!=TipoUsuario.ADMIN) throw new RuntimeException("Acesso permitido somente ao professor"); }
    @GetMapping("/trilhas") public List<Trilha> trilhas() { return trilhas.findAll().stream().filter(Trilha::isAtiva).toList(); }
    @PostMapping("/trilhas") public Trilha criarTrilha(@RequestBody Trilha item, Authentication auth) { professor(auth); item.setId(null); item.setProfessor(user(auth)); return trilhas.save(item); }
    @PutMapping("/trilhas/{id}") public ResponseEntity<Trilha> editarTrilha(@PathVariable Long id,@RequestBody Trilha item,Authentication auth){ professor(auth); return trilhas.findById(id).map(old->{old.setTitulo(item.getTitulo());old.setDescricao(item.getDescricao());old.setConteudoJson(item.getConteudoJson());old.setAtiva(item.isAtiva());return ResponseEntity.ok(trilhas.save(old));}).orElse(ResponseEntity.notFound().build()); }
    @DeleteMapping("/trilhas/{id}") public ResponseEntity<Void> excluirTrilha(@PathVariable Long id,Authentication auth){ professor(auth); if(!trilhas.existsById(id))return ResponseEntity.notFound().build();trilhas.deleteById(id);return ResponseEntity.noContent().build(); }
    @GetMapping("/turmas/{turmaId}/trabalhos") public List<Trabalho> trabalhos(@PathVariable Long turmaId,Authentication auth){ return trabalhos.findByTurmaId(turmaId); }
    @PostMapping("/turmas/{turmaId}/trabalhos") public Trabalho criarTrabalho(@PathVariable Long turmaId,@RequestBody Trabalho item,Authentication auth){ professor(auth); item.setId(null);item.setTurma(turmas.findById(turmaId).orElseThrow(()->new RuntimeException("Turma não encontrada")));item.setProfessor(user(auth));return trabalhos.save(item); }
    @PutMapping("/trabalhos/{id}") public ResponseEntity<Trabalho> editarTrabalho(@PathVariable Long id,@RequestBody Trabalho item,Authentication auth){ professor(auth);return trabalhos.findById(id).map(old->{old.setTitulo(item.getTitulo());old.setDescricao(item.getDescricao());old.setPontuacaoMaxima(item.getPontuacaoMaxima());old.setPrazoEntrega(item.getPrazoEntrega());old.setTrabalhoEquipe(item.isTrabalhoEquipe());old.setChecklistJson(item.getChecklistJson());return ResponseEntity.ok(trabalhos.save(old));}).orElse(ResponseEntity.notFound().build()); }
    @DeleteMapping("/trabalhos/{id}") public ResponseEntity<Void> excluirTrabalho(@PathVariable Long id,Authentication auth){ professor(auth);if(!trabalhos.existsById(id))return ResponseEntity.notFound().build();trabalhos.deleteById(id);return ResponseEntity.noContent().build(); }
    @GetMapping("/trabalhos/{id}/entregas") public List<EntregaTrabalho> entregas(@PathVariable Long id,Authentication auth){ professor(auth);return entregas.findByTrabalhoId(id); }
    @PostMapping("/trabalhos/{id}/entregas") public EntregaTrabalho entregar(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){ Trabalho trabalho=trabalhos.findById(id).orElseThrow(()->new RuntimeException("Trabalho não encontrado")); EntregaTrabalho item=new EntregaTrabalho();item.setTrabalho(trabalho);item.setAluno(user(auth));item.setConteudo(body.getOrDefault("conteudo", ""));return entregas.save(item); }
    @PatchMapping("/entregas/{id}/avaliar") public EntregaTrabalho avaliar(@PathVariable Long id,@RequestBody Map<String,Object> body,Authentication auth){ professor(auth);EntregaTrabalho item=entregas.findById(id).orElseThrow(()->new RuntimeException("Entrega não encontrada"));item.setPontuacao(Integer.valueOf(String.valueOf(body.getOrDefault("pontuacao",0))));item.setFeedback(String.valueOf(body.getOrDefault("feedback", "")));return entregas.save(item); }
    @GetMapping("/turmas/{turmaId}/equipes") public List<Equipe> equipes(@PathVariable Long turmaId,Authentication auth){return equipes.findByTurmaId(turmaId);}
    @PostMapping("/turmas/{turmaId}/equipes") public Equipe criarEquipe(@PathVariable Long turmaId,@RequestBody Equipe item,Authentication auth){item.setId(null);item.setTurma(turmas.findById(turmaId).orElseThrow(()->new RuntimeException("Turma não encontrada")));if(item.getAlunos()!=null&&!item.getAlunos().stream().allMatch(a->a.getTipo()==TipoUsuario.ALUNO))throw new RuntimeException("Equipes aceitam somente alunos");return equipes.save(item);}
    @PatchMapping("/equipes/{id}/checklist") public Equipe atualizarChecklist(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){Equipe item=equipes.findById(id).orElseThrow(()->new RuntimeException("Equipe não encontrada"));item.setChecklistJson(body.getOrDefault("checklistJson", "[]"));return equipes.save(item);}
}
