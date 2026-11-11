package com.github.arthurpaixao.ms.apoio.controller;

import com.github.arthurpaixao.ms.apoio.dto.PessoaApoioRequestDTO;
import com.github.arthurpaixao.ms.apoio.dto.PessoaApoioResponseDTO;
import com.github.arthurpaixao.ms.apoio.entities.StatusSeguranca;
import com.github.arthurpaixao.ms.apoio.service.PessoaApoioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Pessoas de apoio", description = "Pessoas que precisam de ajuda para evacuar")
@RestController
@RequestMapping("/api/v1/pessoas")
public class PessoaApoioController {

    @Autowired
    private PessoaApoioService pessoaApoioService;

    // Testando load balancing: devolve a porta da instancia que respondeu
    @Operation(summary = "Mostra a porta da instância que respondeu (teste de Load Balancer)")
    @GetMapping("/port")
    public ResponseEntity<String> port(@Value("${local.server.port}") String porta) {
        return ResponseEntity.ok("Instância do ms-apoio respondeu na porta " + porta);
    }

    @Operation(summary = "Lista as pessoas por prioridade (filtro opcional por status: painel 'quem ainda falta')")
    @GetMapping
    public ResponseEntity<List<PessoaApoioResponseDTO>> getAllPessoas(
            @RequestParam(required = false) StatusSeguranca status) {
        List<PessoaApoioResponseDTO> list = pessoaApoioService.findAllPessoas(status);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Busca uma pessoa pelo ID")
    @GetMapping("/{id}")
    public ResponseEntity<PessoaApoioResponseDTO> getPessoaById(@PathVariable Long id) {
        PessoaApoioResponseDTO responseDTO = pessoaApoioService.findPessoaById(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Cadastra uma pessoa, com consentimento (status inicial: CADASTRADA)")
    @PostMapping
    public ResponseEntity<PessoaApoioResponseDTO> createPessoa(@RequestBody @Valid
                                                               PessoaApoioRequestDTO requestDTO) {
        PessoaApoioResponseDTO responseDTO = pessoaApoioService.savePessoa(requestDTO);
        URI uri = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(responseDTO.getId())
                .toUri();
        return ResponseEntity.created(uri).body(responseDTO);
    }

    @Operation(summary = "Atualiza os dados de uma pessoa (bloqueado enquanto ela estiver EM_ALERTA)")
    @PutMapping("/{id}")
    public ResponseEntity<PessoaApoioResponseDTO> updatePessoa(@PathVariable Long id,
                                                               @RequestBody @Valid
                                                               PessoaApoioRequestDTO requestDTO) {
        PessoaApoioResponseDTO responseDTO = pessoaApoioService.updatePessoa(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Exclui uma pessoa (bloqueado enquanto ela estiver EM_ALERTA)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePessoa(@PathVariable Long id) {
        pessoaApoioService.deletePessoa(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Endpoints chamados pelo ms-alertas via OpenFeign (comunicacao sincrona) ----

    @Operation(summary = "Coloca a pessoa EM_ALERTA (chamado pelo ms-alertas via OpenFeign)")
    @PutMapping("/{id}/alertar")
    public ResponseEntity<PessoaApoioResponseDTO> alertarPessoa(@PathVariable Long id) {
        PessoaApoioResponseDTO responseDTO = pessoaApoioService.alertarPessoa(id);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Marca a pessoa como SEGURA (chamado pelo ms-alertas via OpenFeign)")
    @PutMapping("/{id}/segura")
    public ResponseEntity<PessoaApoioResponseDTO> marcarPessoaSegura(@PathVariable Long id) {
        PessoaApoioResponseDTO responseDTO = pessoaApoioService.marcarPessoaSegura(id);
        return ResponseEntity.ok(responseDTO);
    }
}
