package com.github.arthurpaixao.ms.alertas.dto;

import com.github.arthurpaixao.ms.alertas.entities.TipoRisco;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TipoRiscoResponseDTO {

    private Long id;
    private String nome;
    private String descricao;

    public TipoRiscoResponseDTO(TipoRisco tipoRisco) {
        id = tipoRisco.getId();
        nome = tipoRisco.getNome();
        descricao = tipoRisco.getDescricao();
    }
}
