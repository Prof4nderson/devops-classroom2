package com.devopsclassroom.repository;
import com.devopsclassroom.entity.Periodo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PeriodoRepository extends JpaRepository<Periodo, Long> { List<Periodo> findByInstituicaoId(Long instituicaoId); }
