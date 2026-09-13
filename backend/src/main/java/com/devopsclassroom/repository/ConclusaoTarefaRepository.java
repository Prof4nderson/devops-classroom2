package com.devopsclassroom.repository;

import com.devopsclassroom.entity.ConclusaoTarefa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConclusaoTarefaRepository extends JpaRepository<ConclusaoTarefa, Long> {
    List<ConclusaoTarefa> findByTrabalhoId(Long trabalhoId);
    Optional<ConclusaoTarefa> findByTrabalhoIdAndEquipeIdAndIndiceTarefa(Long trabalhoId, Long equipeId, Integer indiceTarefa);
    Optional<ConclusaoTarefa> findByTrabalhoIdAndEquipeIsNullAndIndiceTarefa(Long trabalhoId, Integer indiceTarefa);
    void deleteByTrabalhoId(Long trabalhoId);
}
