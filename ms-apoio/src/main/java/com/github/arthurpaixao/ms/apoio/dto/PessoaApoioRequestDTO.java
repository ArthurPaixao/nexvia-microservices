package com.github.arthurpaixao.ms.apoio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada. O status de seguranca NAO e recebido:
 * no cadastro o sistema define CADASTRADA e ele so muda pelo
 * ms-alertas (comunicacao sincrona via OpenFeign).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PessoaApoioRequestDTO {

    @NotBlank(message = "O campo nome é obrigatório")
    @Size(min = 3, max = 100, message = "O campo nome deve ter entre 3 e 100 caracteres")
    @Schema(example = "Maria Aparecida dos Santos")
    private String nome;

    @NotBlank(message = "O campo endereço é obrigatório")
    @Size(min = 5, max = 150, message = "O campo endereço deve ter entre 5 e 150 caracteres")
    @Schema(example = "Rua das Palmeiras, 120 - casa 2")
    private String endereco;

    @NotBlank(message = "O campo bairro é obrigatório")
    @Size(min = 3, max = 80, message = "O campo bairro deve ter entre 3 e 80 caracteres")
    @Schema(example = "Morro da Esperança")
    private String bairro;

    @NotBlank(message = "O campo necessidade é obrigatório")
    @Size(min = 5, max = 255, message = "O campo necessidade deve ter entre 5 e 255 caracteres")
    @Schema(example = "Acamada, 82 anos, usa concentrador de oxigênio")
    private String necessidade;

    @NotNull(message = "O campo prioridade é obrigatório")
    @Min(value = 1, message = "A prioridade deve ser 1 (alta), 2 (média) ou 3 (baixa)")
    @Max(value = 3, message = "A prioridade deve ser 1 (alta), 2 (média) ou 3 (baixa)")
    @Schema(example = "1")
    private Integer prioridade;

    @NotBlank(message = "O campo nome do padrinho é obrigatório")
    @Size(min = 3, max = 100, message = "O campo nome do padrinho deve ter entre 3 e 100 caracteres")
    @Schema(example = "João Pereira")
    private String nomePadrinho;

    @NotBlank(message = "O campo telefone do padrinho é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "O telefone do padrinho deve ter 10 ou 11 dígitos (somente números, com DDD)")
    @Schema(example = "11987654321")
    private String telefonePadrinho;

    @NotNull(message = "O consentimento (LGPD) é obrigatório")
    @AssertTrue(message = "O cadastro só pode ser feito com o consentimento da pessoa ou do responsável (LGPD)")
    @Schema(example = "true")
    private Boolean consentimentoLgpd;
}
