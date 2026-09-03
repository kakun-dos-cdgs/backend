package com.lacasafest.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("mensagem", "Verifique os dados informados.");
        resposta.put("campos", campos);
        return ResponseEntity.badRequest().body(resposta);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratarStatus(ResponseStatusException ex) {
        Map<String, String> resposta = new LinkedHashMap<>();
        resposta.put("mensagem", ex.getReason() != null ? ex.getReason() : "Não foi possível processar a solicitação.");
        return ResponseEntity.status(ex.getStatusCode()).body(resposta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> tratarErroInterno(Exception ex) {
        Map<String, String> resposta = new LinkedHashMap<>();
        resposta.put("mensagem", "Ocorreu um erro interno. Tente novamente mais tarde.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
    }
}
