error id: file:///D:/devops-classroom2-diario-quiz-temas/devops-classroom2-main/backend/src/main/java/com/devopsclassroom/config/SwaggerConfig.java:io/swagger/v3/oas/models/security/SecurityScheme#
file:///D:/devops-classroom2-diario-quiz-temas/devops-classroom2-main/backend/src/main/java/com/devopsclassroom/config/SwaggerConfig.java
empty definition using pc, found symbol in pc: io/swagger/v3/oas/models/security/SecurityScheme#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 268
uri: file:///D:/devops-classroom2-diario-quiz-temas/devops-classroom2-main/backend/src/main/java/com/devopsclassroom/config/SwaggerConfig.java
text:
```scala
package com.devopsclassroom.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.@@SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TI Classroom API")
                        .version("1.0")
                        .description("Documentação das APIs do sistema DevOps Classroom"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: io/swagger/v3/oas/models/security/SecurityScheme#