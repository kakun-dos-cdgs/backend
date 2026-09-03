package com.lacasafest.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ConfirmarOrcamentoRequest(
        @NotNull(message = "O valor do contrato é obrigatório para confirmar")
        @DecimalMin(value = "0.01", message = "O valor do contrato deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O valor do contrato deve ter no máximo 2 casas decimais")
        BigDecimal valorContrato
) {}
