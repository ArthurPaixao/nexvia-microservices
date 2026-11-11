package com.github.arthurpaixao.ms.apoio.exceptions.handler;

import com.github.arthurpaixao.ms.apoio.exceptions.PessoaEmAlertaException;
import com.github.arthurpaixao.ms.apoio.exceptions.ResourceNotFoundException;
import com.github.arthurpaixao.ms.apoio.exceptions.StatusInvalidoException;
import com.github.arthurpaixao.ms.apoio.exceptions.dto.CustomErrorDTO;
import com.github.arthurpaixao.ms.apoio.exceptions.dto.ValidationErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - recurso nao encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CustomErrorDTO> handleResourceNotFound(ResourceNotFoundException e,
                                                                 HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND; //404
        CustomErrorDTO err = new CustomErrorDTO(Instant.now(), status.value(),
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    // 422 - erros de Bean Validation nos DTOs de entrada
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomErrorDTO> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                       HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY; //422
        ValidationErrorDTO err = new ValidationErrorDTO(Instant.now(), status.value(),
                "Dados inválidos", request.getRequestURI());
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            err.addError(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(status).body(err);
    }

    // 400 - JSON malformado / corpo invalido (nao da para desserializar)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CustomErrorDTO> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                                       HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST; //400
        CustomErrorDTO err = new CustomErrorDTO(Instant.now(), status.value(),
                "Requisição inválida (JSON malformado ou corpo não interpretável).",
                request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    // 400 - tipo invalido em PathVariable/RequestParam (ex.: /pessoas/abc quando espera Long)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CustomErrorDTO> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                             HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST; //400
        CustomErrorDTO err = new CustomErrorDTO(Instant.now(), status.value(),
                "Requisição inválida (parâmetro com tipo/formato incorreto).",
                request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    // 409 - pessoa EM_ALERTA nao pode ser alterada ou excluida
    @ExceptionHandler(PessoaEmAlertaException.class)
    public ResponseEntity<CustomErrorDTO> handlePessoaEmAlerta(PessoaEmAlertaException e,
                                                               HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT; //409
        CustomErrorDTO err = new CustomErrorDTO(Instant.now(), status.value(),
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    // 409 - mudanca de status nao permitida
    @ExceptionHandler(StatusInvalidoException.class)
    public ResponseEntity<CustomErrorDTO> handleStatusInvalido(StatusInvalidoException e,
                                                               HttpServletRequest request) {
        HttpStatus status = HttpStatus.CONFLICT; //409
        CustomErrorDTO err = new CustomErrorDTO(Instant.now(), status.value(),
                e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }
}
