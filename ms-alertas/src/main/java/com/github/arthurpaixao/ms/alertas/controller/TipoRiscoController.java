package com.github.arthurpaixao.ms.alertas.controller;

import com.github.arthurpaixao.ms.alertas.dto.TipoRiscoResponseDTO;
import com.github.arthurpaixao.ms.alertas.service.TipoRiscoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tipos de risco", description = "Tipos de risco cadastrados no banco (carga via Flyway)")
@RestController
@RequestMapping("/api/v1/tipos-risco")
public class TipoRiscoController {

    @Autowired
    private TipoRiscoService tipoRiscoService;

    @Operation(summary = "Lista os tipos de risco disponíveis para abrir um acionamento")
    @GetMapping
    public ResponseEntity<List<TipoRiscoResponseDTO>> getAllTiposRisco() {
        return ResponseEntity.ok(tipoRiscoService.findAllTiposRisco());
    }

    // Testando load balancing: devolve a porta da instancia que respondeu
    @Operation(summary = "Mostra a porta da instância que respondeu (teste de Load Balancer)")
    @GetMapping("/port")
    public ResponseEntity<String> port(@Value("${local.server.port}") String porta) {
        return ResponseEntity.ok("Instância do ms-alertas respondeu na porta " + porta);
    }
}
