package com.github.arthurpaixao.ms.alertas.exceptions;

/**
 * Lancada quando a comunicacao sincrona (OpenFeign) com o ms-apoio falha
 * (servico fora do ar, sem instancias no Eureka, timeout...).
 */
public class ComunicacaoException extends RuntimeException {

    public ComunicacaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
