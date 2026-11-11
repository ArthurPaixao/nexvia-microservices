package com.github.arthurpaixao.ms.apoio.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pessoa que nao consegue evacuar sozinha (acamada, idosa que mora sozinha,
 * pessoa com deficiencia, quem depende de aparelho eletrico...).
 * O cadastro so existe com consentimento (LGPD).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "tb_pessoa_apoio")
public class PessoaApoio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 150)
    private String endereco;

    @Column(nullable = false, length = 80)
    private String bairro;

    @Column(nullable = false)
    private String necessidade;

    // 1 = alta, 2 = media, 3 = baixa
    @Column(nullable = false)
    private Integer prioridade;

    @Column(nullable = false, length = 100)
    private String nomePadrinho;

    @Column(nullable = false, length = 11)
    private String telefonePadrinho;

    @Column(nullable = false)
    private Boolean consentimentoLgpd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSeguranca statusSeguranca;
}
