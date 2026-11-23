package com.drone.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI aeroMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DRONE-MONITOR — API de Monitoramento e Gestão de Voos com Drones")
                        .version("1.0.0")
                        .description("API RESTful em Java 21 / Spring Boot para gestão de empresas operadoras, frota ANAC, pilotos (CMA), clientes, operações de voo e auditorias técnicas.")
                        .contact(new Contact().name("Equipe DRONE-MONITOR")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
