package com.devopsclassroom.repository;

import com.devopsclassroom.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByTurmaId(Long turmaId);
    List<Avaliacao> findByAlunoId(Long alunoId);
    List<Avaliacao> findByAlunoIdAndTurmaId(Long alunoId, Long turmaId);
}
