package com.github.arthurpaixao.ms.apoio.config;

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
                        .title("NEXVIA - Microsservico de Apoio")
                        .description("API responsavel pelo cadastro, com consentimento, das pessoas "
                                + "que precisam de ajuda para evacuar, seus padrinhos e o status de seguranca")
                        .version("v1"))

                // nome do microsservico registrado no Eureka (acesso via API Gateway)
                .servers(List.of(
                        new Server().url("/ms-apoio")
                ));
    }
}
