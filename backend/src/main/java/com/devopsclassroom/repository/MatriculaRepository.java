package com.devopsclassroom.repository;

import com.devopsclassroom.entity.Matricula;
import com.devopsclassroom.entity.StatusMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByCursoId(Long cursoId);
    List<Matricula> findByUsuarioId(Long usuarioId);
    List<Matricula> findByTurmaId(Long turmaId);
    List<Matricula> findByTurmaIdAndStatus(Long turmaId, StatusMatricula status);
    Optional<Matricula> findByUsuarioIdAndCursoId(Long usuarioId, Long cursoId);
    boolean existsByUsuarioIdAndCursoId(Long usuarioId, Long cursoId);
    boolean existsByUsuarioIdAndTurmaIdAndStatus(Long usuarioId, Long turmaId, StatusMatricula status);
}
