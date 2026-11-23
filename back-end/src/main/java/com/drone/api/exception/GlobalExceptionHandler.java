package com.drone.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ApiErrorResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                ApiErrorResponse.of(
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Erro de Regra de Negócio",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleNaoEncontrado(
            RecursoNaoEncontradoException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiErrorResponse.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Recurso Não Encontrado",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiErrorResponse> handleConflito(
            ConflitoException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        "Conflito de Dados",
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String primaryMessage = fieldErrors.values().stream()
                .findFirst()
                .orElse("Os dados informados são inválidos.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiErrorResponse.ofValidation(
                        HttpStatus.BAD_REQUEST.value(),
                        "Erro de Validação",
                        primaryMessage,
                        request.getRequestURI(),
                        fieldErrors
                )
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiErrorResponse.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Não Autorizado",
                        "E-mail ou senha inválidos.",
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiErrorResponse> handleDisabled(
            DisabledException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Conta Desativada",
                        "Esta conta foi desativada. Entre em contato com um administrador.",
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        String mensagem = (ex.getMessage() != null && !ex.getMessage().isBlank()
                && !"Access Denied".equalsIgnoreCase(ex.getMessage()))
                ? ex.getMessage()
                : "Você não possui permissão para acessar este recurso.";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiErrorResponse.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Acesso Negado",
                        mensagem,
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Erro Interno",
                        "Ocorreu um erro inesperado ao processar sua solicitação.",
                        request.getRequestURI()
                )
        );
    }
}
