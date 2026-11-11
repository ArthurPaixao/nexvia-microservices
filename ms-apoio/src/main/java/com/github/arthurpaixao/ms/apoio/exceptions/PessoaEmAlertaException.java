package com.github.arthurpaixao.ms.apoio.exceptions;

/**
 * Lancada quando se tenta alterar ou excluir uma pessoa que esta EM_ALERTA:
 * durante o alerta o cadastro fica congelado para o padrinho nao receber dado errado.
 */
public class PessoaEmAlertaException extends RuntimeException {

    public PessoaEmAlertaException(String message) {
        super(message);
    }
}
