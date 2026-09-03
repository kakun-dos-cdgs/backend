package com.lacasafest.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TesteController {

    @GetMapping("/api/teste")
    public Map<String, String> teste() {
        return Map.of("mensagem", "API La Casa Fest funcionando");
    }
}
