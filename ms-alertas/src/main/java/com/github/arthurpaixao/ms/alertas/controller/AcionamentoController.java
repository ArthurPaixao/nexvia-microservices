package com.github.arthurpaixao.ms.alertas.controller;

import com.github.arthurpaixao.ms.alertas.dto.AcionamentoRequestDTO;
import com.github.arthurpaixao.ms.alertas.dto.AcionamentoResponseDTO;
import com.github.arthurpaixao.ms.alertas.exceptions.ResourceNotFoundException;
import com.github.arthurpaixao.ms.alertas.exceptions.StatusInvalidoException;
import com.github.arthurpaixao.ms.alertas.service.AcionamentoService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j // do Lombok
@Tag(name = "Acionamentos", description = "Alertas confirmados: acionamento do padrinho e confirmação 'estou seguro'")
@RestController
@RequestMapping("/api/v1/acionamentos")
public class AcionamentoController {

    @Autowired
    private AcionamentoService acionamentoService;

    @Operation(summary = "Lista todos os acionamentos")
    @GetMapping
    public ResponseEntity<List<AcionamentoResponseDTO>> getAllAcionamentos() {
        List<AcionamentoResponseDTO> list = acionamentoService.findAllAcionamentos();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Busca um acionamento pelo ID")
    @GetMapping("/{id}")
    public ResponseEntity<AcionamentoResponseDTO> getAcionamentoById(@PathVariable Long id) {
        AcionamentoResponseDTO responseDTO = acionamentoService.findAcionamentoById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Abre um acionamento para uma pessoa do ms-apoio (status inicial: ABERTO)")
    @PostMapping
    public ResponseEntity<AcionamentoResponseDTO> createAcionamento(@RequestBody @Valid
                                                                    AcionamentoRequestDTO requestDTO) {
        AcionamentoResponseDTO responseDTO = acionamentoService.saveAcionamento(requestDTO);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(responseDTO.getId())
                .toUri();
        return ResponseEntity.created(uri).body(responseDTO);
    }

    @Operation(summary = "Atualiza um acionamento (somente com status ABERTO)")
    @PutMapping("/{id}")
    public ResponseEntity<AcionamentoResponseDTO> updateAcionamento(@PathVariable Long id,
                                                                    @RequestBody @Valid
                                                                    AcionamentoRequestDTO requestDTO) {
        AcionamentoResponseDTO responseDTO = acionamentoService.updateAcionamento(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Exclui um acionamento (bloqueado com status PADRINHO_ACIONADO)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcionamento(@PathVariable Long id) {
        acionamentoService.deleteAcionamento(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Comunicacao sincrona com o ms-apoio (OpenFeign + Circuit Breaker + Fallback) ----

    @Operation(summary = "Aciona o padrinho: acionamento PADRINHO_ACIONADO e pessoa EM_ALERTA no ms-apoio")
    @PatchMapping("/{id}/acionar")
    @CircuitBreaker(name = "alterarStatusPessoa",
            fallbackMethod = "fallbackAcionarPadrinho")
    public ResponseEntity<AcionamentoResponseDTO> acionarPadrinho(@PathVariable Long id) {
        AcionamentoResponseDTO responseDTO = acionamentoService.acionarPadrinho(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Confirma 'estou seguro': acionamento CONCLUIDO e pessoa SEGURA no ms-apoio")
    @PatchMapping("/{id}/concluir")
    @CircuitBreaker(name = "alterarStatusPessoa",
            fallbackMethod = "fallbackConcluirAcionamento")
    public ResponseEntity<AcionamentoResponseDTO> concluirAcionamento(@PathVariable Long id) {
        AcionamentoResponseDTO responseDTO = acionamentoService.concluirAcionamento(id);
        return ResponseEntity.ok(responseDTO);
    }

    // metodo com a mesma assinatura e tipo de retorno de acionarPadrinho
    public ResponseEntity<AcionamentoResponseDTO> fallbackAcionarPadrinho(Long id, Throwable e) {
        // erros de negocio (404 / 409) nao sao falha de comunicacao: seguem para o GlobalExceptionHandler
        repassarErroDeNegocio(e);

        // Registra o erro para fins de log/observabilidade
        log.error("Falha ao acionar padrinho do acionamento {}. Ativando fallback. Erro: {}", id, e.getMessage());
        AcionamentoResponseDTO responseDTO = acionamentoService.marcarConfirmacaoPendente(id);
        // 503 - explicitar que o servico destino falhou, mas ainda assim enviando o corpo.
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(responseDTO);
    }

    // metodo com a mesma assinatura e tipo de retorno de concluirAcionamento
    public ResponseEntity<AcionamentoResponseDTO> fallbackConcluirAcionamento(Long id, Throwable e) {
        repassarErroDeNegocio(e);

        log.error("Falha ao concluir acionamento {}. Ativando fallback. Erro: {}", id, e.getMessage());
        // o status continua PADRINHO_ACIONADO: a confirmacao pode ser reenviada quando o ms-apoio voltar
        AcionamentoResponseDTO responseDTO = acionamentoService.findAcionamentoById(id);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(responseDTO);
    }

    private void repassarErroDeNegocio(Throwable e) {
        if (e instanceof ResourceNotFoundException || e instanceof StatusInvalidoException) {
            throw (RuntimeException) e;
        }
    }
}
