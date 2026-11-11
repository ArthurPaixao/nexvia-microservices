package com.github.arthurpaixao.ms.alertas.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Acionamento do padrinho de uma pessoa vulneravel durante um alerta.
 * A pessoa fica no ms-apoio: aqui guardamos apenas o pessoaId.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "tb_acionamento")
public class Acionamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pessoaId;

    @ManyToOne
    @JoinColumn(name = "tipo_risco_id", nullable = false)
    private TipoRisco tipoRisco;

    @Column(nullable = false, length = 150)
    private String rua;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gravidade gravidade;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHoraAbertura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusAcionamento status;
}
