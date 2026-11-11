package com.github.arthurpaixao.ms.alertas.exceptions;

/**
 * Lancada quando a mudanca de status do acionamento nao e permitida
 * (ex.: concluir um acionamento cujo padrinho ainda nao foi acionado).
 */
public class StatusInvalidoException extends RuntimeException {

    public StatusInvalidoException(String message) {
        super(message);
    }
}
