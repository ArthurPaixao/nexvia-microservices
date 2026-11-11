package com.github.arthurpaixao.ms.alertas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NEXVIA - Microsservico de Alertas")
                        .description("API responsavel pelos acionamentos: abre o alerta, aciona o padrinho, "
                                + "confirma o 'estou seguro' e altera o status da pessoa no ms-apoio via OpenFeign")
                        .version("v1"))

                // nome do microsservico registrado no Eureka (acesso via API Gateway)
                .servers(List.of(
                        new Server().url("/ms-alertas")
                ));
    }
}
