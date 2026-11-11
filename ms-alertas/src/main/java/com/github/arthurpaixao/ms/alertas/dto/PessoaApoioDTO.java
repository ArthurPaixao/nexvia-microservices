package com.github.arthurpaixao.ms.alertas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Dados da pessoa recebidos do ms-apoio via OpenFeign
 * (apenas os campos que o ms-alertas precisa).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PessoaApoioDTO {

    private Long id;
    private String nome;
    private String statusSeguranca;
}
