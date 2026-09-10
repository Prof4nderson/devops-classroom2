package com.devopsclassroom.repository;

import com.devopsclassroom.entity.Presenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresencaRepository extends JpaRepository<Presenca, Long> {
    List<Presenca> findByAulaId(Long aulaId);
    List<Presenca> findByAulaIdIn(Collection<Long> aulaIds);
    List<Presenca> findByUsuarioId(Long usuarioId);
    Optional<Presenca> findByUsuarioIdAndAulaId(Long usuarioId, Long aulaId);
    boolean existsByUsuarioIdAndAulaId(Long usuarioId, Long aulaId);
}
