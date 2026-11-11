package com.github.arthurpaixao.ms.alertas.dto;

import com.github.arthurpaixao.ms.alertas.entities.Gravidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada. O status NAO e recebido: no cadastro o sistema define ABERTO
 * e ele muda pelos endpoints de acionar / concluir.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AcionamentoRequestDTO {

    @NotNull(message = "O campo pessoaId é obrigatório")
    @Positive(message = "O campo pessoaId deve ser um número positivo")
    @Schema(example = "1")
    private Long pessoaId;

    @NotNull(message = "O campo tipoRiscoId é obrigatório")
    @Schema(example = "1")
    private Long tipoRiscoId;

    @NotBlank(message = "O campo rua é obrigatório")
    @Size(min = 3, max = 150, message = "O campo rua deve ter entre 3 e 150 caracteres")
    @Schema(example = "Rua das Palmeiras")
    private String rua;

    @NotNull(message = "O campo gravidade é obrigatório (BAIXA, MEDIA ou ALTA)")
    @Schema(example = "ALTA")
    private Gravidade gravidade;

    @NotBlank(message = "O campo descrição é obrigatório")
    @Size(min = 10, max = 255, message = "O campo descrição deve ter entre 10 e 255 caracteres")
    @Schema(example = "Três moradores relatam rachaduras na encosta atrás das casas")
    private String descricao;
}
