package com.github.arthurpaixao.api.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Monta o Swagger UI do Gateway com a documentacao de todos os
 * microsservicos registrados no Eureka (Swagger dinamico).
 */
@Component
@EnableScheduling
public class DynamicSwaggerConfig {

    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;

    public DynamicSwaggerConfig(
            DiscoveryClient discoveryClient,
            SwaggerUiConfigProperties swaggerUiConfigProperties) {

        this.discoveryClient = discoveryClient;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void carregarAoIniciar() {
        atualizarUrls();
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 10000)
    public void atualizarUrls() {

        Set<SwaggerUrl> urls = discoveryClient.getServices()
                .stream()
                .filter(service ->
                        !service.equalsIgnoreCase("api-gateway"))
                .sorted()
                .map(service -> new SwaggerUrl(
                        service,
                        "/" + service + "/v3/api-docs",
                        service
                ))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        swaggerUiConfigProperties.setUrl(null);
        swaggerUiConfigProperties.setUrls(urls);
    }
}
