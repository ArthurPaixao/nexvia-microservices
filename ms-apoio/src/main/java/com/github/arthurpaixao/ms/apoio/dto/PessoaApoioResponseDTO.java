package com.github.arthurpaixao.ms.apoio.dto;

import com.github.arthurpaixao.ms.apoio.entities.PessoaApoio;
import com.github.arthurpaixao.ms.apoio.entities.StatusSeguranca;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PessoaApoioResponseDTO {

    private Long id;
    private String nome;
    private String endereco;
    private String bairro;
    private String necessidade;
    private Integer prioridade;
    private String nomePadrinho;
    private String telefonePadrinho;
    private Boolean consentimentoLgpd;
    private StatusSeguranca statusSeguranca;

    public PessoaApoioResponseDTO(PessoaApoio pessoa) {
        id = pessoa.getId();
        nome = pessoa.getNome();
        endereco = pessoa.getEndereco();
        bairro = pessoa.getBairro();
        necessidade = pessoa.getNecessidade();
        prioridade = pessoa.getPrioridade();
        nomePadrinho = pessoa.getNomePadrinho();
        telefonePadrinho = pessoa.getTelefonePadrinho();
        consentimentoLgpd = pessoa.getConsentimentoLgpd();
        statusSeguranca = pessoa.getStatusSeguranca();
    }
}
