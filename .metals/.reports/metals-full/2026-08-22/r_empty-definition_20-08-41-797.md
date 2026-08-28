error id: file:///D:/devops-classroom-main-postgresql-fixed/devops-classroom-main/backend/src/main/java/com/devopsclassroom/repository/MensagemRepository.java:org/springframework/data/jpa/repository/JpaRepository#
file:///D:/devops-classroom-main-postgresql-fixed/devops-classroom-main/backend/src/main/java/com/devopsclassroom/repository/MensagemRepository.java
empty definition using pc, found symbol in pc: org/springframework/data/jpa/repository/JpaRepository#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 132
uri: file:///D:/devops-classroom-main-postgresql-fixed/devops-classroom-main/backend/src/main/java/com/devopsclassroom/repository/MensagemRepository.java
text:
```scala
package com.devopsclassroom.repository;

import com.devopsclassroom.entity.Mensagem;
import org.springframework.data.jpa.repository.@@JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
   List<Mensagem> findByAulaIdOrderByCriadoEmAsc(Long aulaId);
   // List<Mensagem> findByAulaIdOrderByCriadoEmAsc(Long aulaId, com.devopsclassroom.entity.TipoMensagem tipo);
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: org/springframework/data/jpa/repository/JpaRepository#