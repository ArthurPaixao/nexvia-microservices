package com.github.arthurpaixao.ms.alertas.dto;

import com.github.arthurpaixao.ms.alertas.entities.Acionamento;
import com.github.arthurpaixao.ms.alertas.entities.Gravidade;
import com.github.arthurpaixao.ms.alertas.entities.StatusAcionamento;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AcionamentoResponseDTO {

    private Long id;
    private Long pessoaId;
    private TipoRiscoResponseDTO tipoRisco;
    private String rua;
    private Gravidade gravidade;
    private String descricao;
    private LocalDateTime dataHoraAbertura;
    private StatusAcionamento status;

    public AcionamentoResponseDTO(Acionamento acionamento) {
        id = acionamento.getId();
        pessoaId = acionamento.getPessoaId();
        tipoRisco = new TipoRiscoResponseDTO(acionamento.getTipoRisco());
        rua = acionamento.getRua();
        gravidade = acionamento.getGravidade();
        descricao = acionamento.getDescricao();
        dataHoraAbertura = acionamento.getDataHoraAbertura();
        status = acionamento.getStatus();
    }
}
