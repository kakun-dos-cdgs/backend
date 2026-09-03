package com.lacasafest.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.lacasafest.backend.dto.ConfirmarOrcamentoRequest;
import com.lacasafest.backend.dto.CriarOrcamentoRequest;
import com.lacasafest.backend.entity.Orcamento;
import com.lacasafest.backend.entity.Orcamento.Status;
import com.lacasafest.backend.repository.OrcamentoRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

// Adicione aqui o endereco do painel secundario quando ele for rodar em outra porta/dominio,
// por exemplo "http://localhost:5500" se for so um HTML aberto local, ou o dominio dele se for hospedado.
@CrossOrigin(origins = { "https://lacasa-fest-ha4k56kx.manus.space"})
@RestController
@RequestMapping("/api/orcamentos")
public class OrcamentoController {

    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final List<Status> STATUS_QUE_BLOQUEIAM = List.of(Status.PENDENTE, Status.CONFIRMADO);

    private final OrcamentoRepository repository;
    private final ConcurrentHashMap<String, Window> requests = new ConcurrentHashMap<>();

    @Value("${app.admin-token:}")
    private String adminToken;

    // Numero do WhatsApp da La Casa Fest, formato internacional sem espacos/simbolos, ex: 5511999999999
    @Value("${app.whatsapp-numero:}")
    private String whatsappNumero;

    public OrcamentoController(OrcamentoRepository repository) {
        this.repository = repository;
    }

    // ---------- PÚBLICO ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Orcamento criar(@Valid @RequestBody CriarOrcamentoRequest dados,
                           HttpServletRequest servletRequest) {
        aplicarRateLimit(obterIpCliente(servletRequest));

        Orcamento orcamento = new Orcamento();
        orcamento.setNome(dados.nome());
        orcamento.setTelefone(dados.telefone());
        orcamento.setTipoEvento(dados.tipoEvento());
        orcamento.setDataEvento(dados.dataEvento());
        orcamento.setQuantidadeConvidados(dados.quantidadeConvidados());
        orcamento.setMensagem(dados.mensagem());
        orcamento.setValorContrato(null);

        // Garante status inicial
        orcamento.setStatus(Status.PENDENTE);

        // Não permite data já reservada (PENDENTE ou CONFIRMADO)
        if (repository.existsByDataEventoAndStatusIn(orcamento.getDataEvento(), STATUS_QUE_BLOQUEIAM)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta data já está reservada. Escolha outra data.");
        }

        Orcamento salvo = repository.save(orcamento);
        salvo.setLinkWhatsapp(montarLinkWhatsapp(salvo));
        return salvo;
    }

    // Monta o link wa.me com a mensagem ja preenchida, pra abrir o WhatsApp
    // com o texto pronto e o cliente so precisar apertar enviar.
    private String montarLinkWhatsapp(Orcamento orcamento) {
        if (whatsappNumero == null || whatsappNumero.isBlank()) {
            return null;
        }
        String dataFormatada = orcamento.getDataEvento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String mensagem = "Olá! Meu nome é %s e gostaria de fechar o orçamento para %s no dia %s (%d convidados)."
                .formatted(orcamento.getNome(), orcamento.getTipoEvento(), dataFormatada,
                        orcamento.getQuantidadeConvidados());
        String mensagemCodificada = URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
        return "https://wa.me/" + whatsappNumero + "?text=" + mensagemCodificada;
    }

    /**
     * Endpoint público: retorna as datas que estão bloqueadas (pendentes + confirmadas).
     * O front pode usar isso para desabilitar essas datas no calendário.
     */
    @GetMapping("/datas-bloqueadas")
    public List<LocalDate> listarDatasBloqueadas(HttpServletRequest request) {
        aplicarRateLimit(obterIpCliente(request));
        return repository.findDatasBloqueadas(STATUS_QUE_BLOQUEIAM);
    }

    // ---------- ADMIN (precisa do token) ----------

    @GetMapping
    public List<Orcamento> listar(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        exigirToken(token);
        return repository.findAll();
    }

    /**
     * Confirma o contrato com o cliente.
     * A data continua bloqueada.
     */
    @PutMapping("/{id}/confirmar")
    public Orcamento confirmar(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmarOrcamentoRequest dados,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        exigirToken(token);

        Orcamento orcamento = buscarOuFalhar(id);

        // Trata null (registros antigos) como PENDENTE
        Status atual = orcamento.getStatus() != null ? orcamento.getStatus() : Status.PENDENTE;

        if (atual == Status.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não é possível confirmar um orçamento que já foi cancelado.");
        }
        if (atual == Status.CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este orçamento já está confirmado.");
        }

        orcamento.setValorContrato(dados.valorContrato());
        orcamento.setStatus(Status.CONFIRMADO);
        return repository.save(orcamento);
    }

    /**
     * Cancela o orçamento: marca como CANCELADO (mantém o histórico) e libera a data,
     * já que datas com status CANCELADO não entram no bloqueio.
     */
    @PutMapping("/{id}/cancelar")
    public Orcamento cancelar(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        exigirToken(token);

        Orcamento orcamento = buscarOuFalhar(id);
        orcamento.setStatus(Status.CANCELADO);
        return repository.save(orcamento);
    }

    // ---------- helpers ----------

    private Orcamento buscarOuFalhar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Orçamento não encontrado."));
    }

    private void exigirToken(String token) {
        if (!tokenValido(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Acesso administrativo não autorizado");
        }
    }

    private boolean tokenValido(String token) {
        if (adminToken == null || adminToken.isBlank() || token == null) {
            return false;
        }
        // Comparacao em tempo constante: evita timing attack
        byte[] esperado = adminToken.getBytes(StandardCharsets.UTF_8);
        byte[] recebido = token.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperado, recebido);
    }

    private String obterIpCliente(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void aplicarRateLimit(String ip) {
        long agora = Instant.now().toEpochMilli();
        limparJanelasExpiradas(agora);
        requests.compute(ip, (chave, janela) -> {
            if (janela == null || agora - janela.inicio >= 60_000) {
                return new Window(agora, 1);
            }
            if (janela.quantidade >= MAX_REQUESTS_PER_MINUTE) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas solicitações. Tente novamente em alguns minutos.");
            }
            janela.quantidade++;
            return janela;
        });
    }

    private void limparJanelasExpiradas(long agora) {
        requests.entrySet().removeIf(entrada -> agora - entrada.getValue().inicio >= 60_000);
    }

    private static class Window {
        private final long inicio;
        private int quantidade;

        private Window(long inicio, int quantidade) {
            this.inicio = inicio;
            this.quantidade = quantidade;
        }
    }
}
