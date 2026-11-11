package com.github.arthurpaixao.ms.apoio.exceptions;

/**
 * Lancada quando a mudanca de status de seguranca nao e permitida
 * (ex.: marcar como SEGURA uma pessoa que nao esta EM_ALERTA).
 */
public class StatusInvalidoException extends RuntimeException {

    public StatusInvalidoException(String message) {
        super(message);
    }
}
