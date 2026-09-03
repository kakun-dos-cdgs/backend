package com.lacasafest.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarOrcamentoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,
        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(regexp = "^[0-9()\\s+\\-]{10,20}$", message = "Informe um telefone válido")
        String telefone,
        @NotBlank(message = "O tipo do evento é obrigatório")
        @Size(max = 80, message = "O tipo do evento deve ter no máximo 80 caracteres")
        String tipoEvento,
        @NotNull(message = "A data do evento é obrigatória")
        @FutureOrPresent(message = "A data do evento não pode estar no passado")
        LocalDate dataEvento,
        @NotNull(message = "A quantidade de convidados é obrigatória")
        @Min(value = 1, message = "A quantidade de convidados deve ser maior que zero")
        Integer quantidadeConvidados,
        @Size(max = 1000, message = "A mensagem deve ter no máximo 1000 caracteres")
        String mensagem
) {}
