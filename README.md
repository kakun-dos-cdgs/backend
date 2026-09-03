# La Casa Fest — API de Orçamentos

**Backend:** Java 21 + Spring Boot  
**Banco de dados:** MySQL 8  
**Versão local:** `http://localhost:8080`

## Visão geral

Esta API recebe e armazena solicitações de orçamento para locação do espaço La Casa Fest.

- Ao criar um orçamento, a **data fica reservada** (status `PENDENTE`).
- No painel administrativo (front separado + token) você pode **confirmar** ou **cancelar**.
- Se cancelar, a data **libera** de novo para outros clientes.
- Datas com status `PENDENTE` ou `CONFIRMADO` não podem ser escolhidas novamente.

## Como executar

Crie o banco de dados no MySQL:

```sql
CREATE DATABASE IF NOT EXISTS lacasa_fest;
```

Configure as variáveis de ambiente antes de iniciar a aplicação. **Nunca** coloque senhas ou tokens no código:

```text
DB_USERNAME=root
DB_PASSWORD=sua_senha_do_mysql
ADMIN_TOKEN=um_token_secreto_para_consulta
```

No Eclipse: **Run Configurations > Environment**. No terminal Windows:

```bat
set DB_USERNAME=root
set DB_PASSWORD=sua_senha_do_mysql
set ADMIN_TOKEN=um_token_secreto_para_consulta
```

## Rotas

| Verbo HTTP | Rota | Token? | Finalidade |
|---|---|---|---|
| `POST` | `/api/orcamentos` | Não | Criar solicitação de orçamento |
| `GET` | `/api/orcamentos/datas-bloqueadas` | Não | Listar datas já reservadas (calendário do site) |
| `GET` | `/api/orcamentos` | Sim (`X-Admin-Token`) | Listar todos os orçamentos |
| `PUT` | `/api/orcamentos/{id}/confirmar` | Sim | Confirmar contrato (status → CONFIRMADO) |
| `PUT` | `/api/orcamentos/{id}/cancelar` | Sim | Cancelar e liberar a data (status → CANCELADO) |
| `GET` | `/api/teste` | Não | Health check |

---

### Criar orçamento (público)

```http
POST /api/orcamentos
Content-Type: application/json
```

```json
{
  "nome": "Maria da Silva",
  "telefone": "11999999999",
  "tipoEvento": "Aniversário",
  "dataEvento": "2026-11-20",
  "quantidadeConvidados": 50,
  "mensagem": "Gostaria de saber os valores."
}
```

- Resposta de sucesso: `201 Created` (status inicial = `PENDENTE`).
- Se a data já estiver reservada: `409 Conflict` com a mensagem *"Esta data já está reservada. Escolha outra data."*
- Rate limit: **5 solicitações por minuto por IP**.

### Datas bloqueadas (público)

```http
GET /api/orcamentos/datas-bloqueadas
```

Retorna um array de datas (`YYYY-MM-DD`) que estão `PENDENTE` ou `CONFIRMADO`.  
Use no front para desabilitar essas datas no calendário.

### Listar orçamentos (admin)

```http
GET /api/orcamentos
X-Admin-Token: um_token_secreto_para_consulta
```

### Confirmar contrato (admin)

```http
PUT /api/orcamentos/123/confirmar
X-Admin-Token: um_token_secreto_para_consulta
```

A data continua bloqueada.

### Cancelar orçamento (admin)

```http
PUT /api/orcamentos/123/cancelar
X-Admin-Token: um_token_secreto_para_consulta
```

A data é liberada e pode ser escolhida por outro cliente.

---

## Status do orçamento

| Status | Significado | Bloqueia a data? |
|---|---|---|
| `PENDENTE` | Cliente solicitou, ainda não fechou contrato | Sim |
| `CONFIRMADO` | Contrato fechado | Sim |
| `CANCELADO` | Cancelado pelo admin (ou não fechou) | **Não** |

## Validações

São rejeitados: nome/telefone/tipo vazios; telefone fora do formato; data no passado; quantidade de convidados &lt; 1; tamanhos máximos de campos.

Erros de validação → `400 Bad Request`.  
Data já ocupada → `409 Conflict`.  
Sem token no admin → `401 Unauthorized`.  
Rate limit → `429 Too Many Requests`.

## Integração com o frontend público

```javascript
// 1. Buscar datas bloqueadas para o calendário
const bloqueadas = await fetch("https://SUA-API.com/api/orcamentos/datas-bloqueadas")
  .then(r => r.json());

// 2. Enviar solicitação
fetch("https://SUA-API.com/api/orcamentos", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    nome,
    telefone,
    tipoEvento,
    dataEvento,
    quantidadeConvidados,
    mensagem
  })
});
```

## Integração com o painel admin (front separado / local)

O token **nunca** vai no site público. Só no painel local do dono.

```javascript
const headers = {
  "Content-Type": "application/json",
  "X-Admin-Token": "um_token_secreto_para_consulta" // só no painel local
};

// Listar
const lista = await fetch("https://SUA-API.com/api/orcamentos", { headers }).then(r => r.json());

// Confirmar
await fetch(`https://SUA-API.com/api/orcamentos/${id}/confirmar`, {
  method: "PUT",
  headers
});

// Cancelar (libera a data)
await fetch(`https://SUA-API.com/api/orcamentos/${id}/cancelar`, {
  method: "PUT",
  headers
});
```

## Configuração de produção

- Use HTTPS
- `DDL_AUTO=validate`
- Mantenha `DB_PASSWORD` e `ADMIN_TOKEN` em variáveis secretas
- Limite o CORS só aos domínios oficiais + o origin do painel admin se necessário
- Faça backups regulares do banco
