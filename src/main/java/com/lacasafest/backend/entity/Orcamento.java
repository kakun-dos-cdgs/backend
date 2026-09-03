package com.lacasafest.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "orcamentos")
public class Orcamento {

    public enum Status {
        PENDENTE,
        CONFIRMADO,
        CANCELADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
    private String nome;

    @NotBlank(message = "O telefone é obrigatório")
    @Pattern(regexp = "^[0-9()\\s+\\-]{10,20}$", message = "Informe um telefone válido")
    private String telefone;

    @NotBlank(message = "O tipo do evento é obrigatório")
    @Size(max = 80, message = "O tipo do evento deve ter no máximo 80 caracteres")
    private String tipoEvento;

    @NotNull(message = "A data do evento é obrigatória")
    @FutureOrPresent(message = "A data do evento não pode estar no passado")
    private LocalDate dataEvento;

    @NotNull(message = "A quantidade de convidados é obrigatória")
    @Min(value = 1, message = "A quantidade de convidados deve ser maior que zero")
    private Integer quantidadeConvidados;

    @Size(max = 1000, message = "A mensagem deve ter no máximo 1000 caracteres")
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDENTE;

    @Digits(integer = 10, fraction = 2,
            message = "O valor do contrato deve ter no máximo 2 casas decimais")
    @Column(name = "valor_contrato", precision = 12, scale = 2)
    private BigDecimal valorContrato;

    // Não é salvo no banco - montado pelo backend só na resposta do POST,
    // pra abrir o WhatsApp já com a mensagem de fechamento de contrato pronta.
    @Transient
    private String linkWhatsapp;

    public Orcamento() {}

    public Long getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public LocalDate getDataEvento() { return dataEvento; }
    public void setDataEvento(LocalDate dataEvento) { this.dataEvento = dataEvento; }

    public Integer getQuantidadeConvidados() { return quantidadeConvidados; }
    public void setQuantidadeConvidados(Integer quantidadeConvidados) { this.quantidadeConvidados = quantidadeConvidados; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getValorContrato() { return valorContrato; }
    public void setValorContrato(BigDecimal valorContrato) { this.valorContrato = valorContrato; }

    public String getLinkWhatsapp() { return linkWhatsapp; }
    public void setLinkWhatsapp(String linkWhatsapp) { this.linkWhatsapp = linkWhatsapp; }
}
