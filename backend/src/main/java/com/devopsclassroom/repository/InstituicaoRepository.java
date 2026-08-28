package com.devopsclassroom.repository;
import com.devopsclassroom.entity.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> { }
