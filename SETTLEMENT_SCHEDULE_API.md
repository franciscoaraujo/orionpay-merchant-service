# Agenda de Liquidação (Settlement Schedule) - Documentação

## Visão Geral

Este documento descreve os endpoints da agenda de liquidação (Settlement Schedule) da OrionPay Merchant Service. Esses endpoints permitem que os lojistas visualizem sua agenda financeira de forma detalhada e agrupada por data esperada de liquidação.

## Endpoints

### 1. GET /api/v1/settlements/schedule

**Descrição:** Retorna a agenda financeira detalhada agrupada por data esperada de liquidação.

**Método HTTP:** GET

**URL:** `/api/v1/settlements/schedule`

**Parâmetros de Consulta:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `startDate` | LocalDate (ISO 8601) | ✅ Sim | Data inicial do período (formato: YYYY-MM-DD) |
| `endDate` | LocalDate (ISO 8601) | ✅ Sim | Data final do período (formato: YYYY-MM-DD) |
| `status` | String | ❌ Não | Status específico para filtrar. Valores válidos: `PENDING`, `SCHEDULED`, `ANTICIPATED`, `BLOCKED`, `SETTLED`, `PAID`, `DISPUTE`, `FAILED`, `PREPAID` |

**Status HTTP de Resposta:**
- `200 OK` - Requisição bem-sucedida
- `400 BAD REQUEST` - Parâmetros inválidos (datas nulas, startDate > endDate)
- `401 UNAUTHORIZED` - Não autenticado
- `403 FORBIDDEN` - Sem permissão para acessar este lojista
- `500 INTERNAL SERVER ERROR` - Erro no servidor

**Exemplo de Requisição:**

```bash
# Buscar agenda para janeiro/2024
GET /api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31 HTTP/1.1
Authorization: Bearer <token>

# Buscar apenas transações agendadas
GET /api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31&status=SCHEDULED HTTP/1.1
Authorization: Bearer <token>

# Buscar apenas transações antecipadas
GET /api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31&status=ANTICIPATED HTTP/1.1
Authorization: Bearer <token>
```

**Exemplo de Resposta (200 OK):**

```json
{
  "periodStart": "2024-01-01",
  "periodEnd": "2024-01-31",
  "totalPeriodGross": "50000.00",
  "totalPeriodNet": "48500.00",
  "totalTransactionsInPeriod": 215,
  "schedule": [
    {
      "date": "2024-01-05",
      "totalGross": "2500.00",
      "totalNet": "2425.00",
      "mdrAmount": "75.00",
      "statusSummary": ["SCHEDULED", "PENDING"],
      "transactionCount": 12,
      "dailyAverageTransaction": "208.33"
    },
    {
      "date": "2024-01-10",
      "totalGross": "5000.00",
      "totalNet": "4850.00",
      "mdrAmount": "150.00",
      "statusSummary": ["SCHEDULED", "SETTLED"],
      "transactionCount": 23,
      "dailyAverageTransaction": "217.39"
    },
    {
      "date": "2024-01-15",
      "totalGross": "3500.00",
      "totalNet": "3395.00",
      "mdrAmount": "105.00",
      "statusSummary": ["SCHEDULED"],
      "transactionCount": 18,
      "dailyAverageTransaction": "194.44"
    }
  ]
}
```

**Campos da Resposta:**

- `periodStart`: Data inicial do período consultado
- `periodEnd`: Data final do período consultado
- `totalPeriodGross`: Soma de todas as transações no período (valor bruto)
- `totalPeriodNet`: Soma de todas as transações no período (valor líquido após MDR)
- `totalTransactionsInPeriod`: Quantidade total de transações no período
- `schedule`: Array com os dados agrupados por dia

**Campos de cada DailySchedule:**

- `date`: Data esperada de liquidação
- `totalGross`: Soma do valor bruto naquela data
- `totalNet`: Soma do valor líquido naquela data
- `mdrAmount`: Total de MDR (taxa) naquela data
- `statusSummary`: Conjunto de status presentes naquela data
- `transactionCount`: Quantidade de transações naquela data
- `dailyAverageTransaction`: Ticket médio (valor bruto / quantidade)

---

### 2. GET /api/v1/settlements/schedule/{settlementDate}

**Descrição:** Retorna o detalhe detalhado de todas as transações/parcelas de um dia específico na agenda de liquidação.

**Método HTTP:** GET

**URL:** `/api/v1/settlements/schedule/{settlementDate}`

**Parâmetros de Path:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `settlementDate` | LocalDate (ISO 8601) | ✅ Sim | Data de liquidação desejada (formato: YYYY-MM-DD) |

**Parâmetros de Consulta:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|-----------|------|-------------|-----------|
| `status` | String | ❌ Não | Status específico para filtrar |
| `page` | Integer | ❌ Não | Número da página (padrão: 0) |
| `size` | Integer | ❌ Não | Tamanho da página (padrão: 50) |
| `sort` | String | ❌ Não | Campo para ordenação (padrão: settlementDate,DESC) |

**Status HTTP de Resposta:**
- `200 OK` - Requisição bem-sucedida
- `400 BAD REQUEST` - Data inválida
- `401 UNAUTHORIZED` - Não autenticado
- `403 FORBIDDEN` - Sem permissão
- `500 INTERNAL SERVER ERROR` - Erro no servidor

**Exemplo de Requisição:**

```bash
# Obter detalhe completo de um dia
GET /api/v1/settlements/schedule/2024-01-15 HTTP/1.1
Authorization: Bearer <token>

# Obter apenas transações SCHEDULED daquele dia, página 1
GET /api/v1/settlements/schedule/2024-01-15?status=SCHEDULED&page=1&size=20 HTTP/1.1
Authorization: Bearer <token>

# Obter apenas transações antecipadas
GET /api/v1/settlements/schedule/2024-01-15?status=ANTICIPATED&page=0&size=50 HTTP/1.1
Authorization: Bearer <token>
```

**Exemplo de Resposta (200 OK):**

```json
{
  "settlementDate": "2024-01-15",
  "totalGross": "3500.00",
  "totalMdr": "105.00",
  "totalNet": "3395.00",
  "averageTransaction": "194.44",
  "totalCount": 18,
  "blockedCount": 1,
  "anticipatedCount": 2,
  "statusBreakdown": {
    "SCHEDULED": 15,
    "BLOCKED": 1,
    "ANTICIPATED": 2
  },
  "pageNumber": 0,
  "pageSize": 50,
  "totalPages": 1,
  "totalElements": 18,
  "transactions": [
    {
      "idExt": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "transactionId": "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6",
      "nsu": "123456",
      "transactionDate": "2024-01-10T14:30:00",
      "settlementDate": "2024-01-15T00:00:00",
      "paidAt": null,
      "grossAmount": "250.00",
      "originalAmount": "250.00",
      "mdrPercentage": "2.99",
      "mdrAmount": "7.50",
      "netAmount": "242.50",
      "cardBrand": "VISA",
      "cardLastFour": "1234",
      "productType": "CREDIT_CARD",
      "blocked": false,
      "anticipated": false,
      "installmentNumber": 1,
      "installmentLabel": "À vista",
      "status": "SCHEDULED"
    },
    {
      "idExt": "a47ac10b-58cc-4372-a567-0e02b2c3d480",
      "transactionId": "b2c3d4e5-f6a7-48b9-c0d1-e2f3a4b5c6d7",
      "nsu": "123457",
      "transactionDate": "2024-01-10T15:45:00",
      "settlementDate": "2024-01-15T00:00:00",
      "paidAt": null,
      "grossAmount": "500.00",
      "originalAmount": "1500.00",
      "mdrPercentage": "2.99",
      "mdrAmount": "14.95",
      "netAmount": "485.05",
      "cardBrand": "MASTERCARD",
      "cardLastFour": "5678",
      "productType": "CREDIT_CARD",
      "blocked": false,
      "anticipated": true,
      "installmentNumber": 1,
      "installmentLabel": "1/3",
      "status": "ANTICIPATED"
    }
  ]
}
```

**Campos da Resposta:**

- `settlementDate`: Data de liquidação do detalhe
- `totalGross`: Soma total das transações daquele dia
- `totalMdr`: Total de MDR naquele dia
- `totalNet`: Valor líquido total naquele dia
- `averageTransaction`: Ticket médio daquele dia
- `totalCount`: Quantidade total de transações
- `blockedCount`: Quantidade de transações bloqueadas
- `anticipatedCount`: Quantidade de transações antecipadas
- `statusBreakdown`: Mapa com contagem por status
- `pageNumber`: Número da página atual (0-indexed)
- `pageSize`: Tamanho da página
- `totalPages`: Total de páginas disponíveis
- `totalElements`: Total de elementos no banco
- `transactions`: Array com detalhes de cada transação

**Campos de cada TransactionDetail:**

- `idExt`: ID externo (UUID da entrada de liquidação)
- `transactionId`: ID da transação original
- `nsu`: Número Sequencial Único (referência no adquirente)
- `transactionDate`: Data/hora da transação
- `settlementDate`: Data/hora esperada de liquidação
- `paidAt`: Data/hora efetiva do pagamento (null se não pago)
- `grossAmount`: Valor da parcela
- `originalAmount`: Valor total da venda
- `mdrPercentage`: Percentual de MDR aplicado
- `mdrAmount`: Valor absoluto de MDR
- `netAmount`: Valor líquido (gross - mdr)
- `cardBrand`: Bandeira do cartão (VISA, MASTERCARD, etc)
- `cardLastFour`: Últimos 4 dígitos do cartão
- `productType`: Tipo de produto (CREDIT_CARD, DEBIT_CARD, etc)
- `blocked`: Se a transação está bloqueada
- `anticipated`: Se a transação foi antecipada
- `installmentNumber`: Número da parcela
- `installmentLabel`: Rótulo da parcela (ex: "1/12", "À vista")
- `status`: Status atual da transação

---

## Casos de Uso

### Caso 1: Visualizar Agenda do Mês

```bash
# Listar todos os dias do mês de janeiro/2024
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer <seu_token>"
```

**Resposta:** Retorna a agenda agrupada por dia com totalizadores agregados.

---

### Caso 2: Filtrar por Status Específico

```bash
# Ver apenas as transações que já foram liquidadas
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31&status=SETTLED" \
  -H "Authorization: Bearer <seu_token>"

# Ver apenas as transações antecipadas
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31&status=ANTICIPATED" \
  -H "Authorization: Bearer <seu_token>"

# Ver apenas as transações em disputa (chargeback)
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31&status=DISPUTE" \
  -H "Authorization: Bearer <seu_token>"
```

---

### Caso 3: Expandir Detalhe de um Dia Específico

```bash
# Primeiro, obter a agenda do período
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2024-01-01&endDate=2024-01-31" \
  -H "Authorization: Bearer <seu_token>"

# Depois, clicar em um dia para ver o detalhe
curl -X GET "http://localhost:8080/api/v1/settlements/schedule/2024-01-15" \
  -H "Authorization: Bearer <seu_token>"
```

**Resposta:** Retorna lista detalhada de transações daquele dia com informações de cartão, MDR, status, etc.

---

### Caso 4: Filtrar Detalhe com Paginação

```bash
# Detalhe de um dia, filtrando apenas agendadas, página 2, 10 items por página
curl -X GET "http://localhost:8080/api/v1/settlements/schedule/2024-01-15?status=SCHEDULED&page=1&size=10" \
  -H "Authorization: Bearer <seu_token>"
```

---

## Enum SettlementStatus

| Status | Descrição |
|--------|-----------|
| `PENDING` | Criado, aguardando Ledger (contabilização) |
| `SCHEDULED` | Contabilizado, aguardando vencimento |
| `ANTICIPATED` | Antecipado pelo lojista (recebeu dinheiro antes) |
| `BLOCKED` | Travado (por Garantia de Empréstimo ou Fraude) |
| `SETTLED` | Pronto para payout (transferência) |
| `PAID` | Pago efetivamente ao lojista |
| `DISPUTE` | Em disputa (Chargeback recebido) |
| `FAILED` | Erro de processamento |
| `PREPAID` | Liquidado antecipadamente |

---

## Fluxo Típico de uma Transação

```
PENDING 
  ↓
SCHEDULED (após contabilização no Ledger)
  ↓
SETTLED (quando vencimento chega)
  ↓
PAID (quando transferência é efetivada)
```

**Desvios Possíveis:**

- PENDING → BLOCKED (bloqueio por fraude/garantia)
- SCHEDULED → ANTICIPATED (lojista antecipa)
- SETTLED → DISPUTE (chargeback recebido)
- PENDING → FAILED (erro no processamento)

---

## Tratamento de Erros

### Erro 400 - Bad Request

```json
{
  "errorCode": "INVALID_DATE_RANGE",
  "message": "Data inicial não pode ser posterior à data final"
}
```

### Erro 401 - Unauthorized

```json
{
  "errorCode": "UNAUTHORIZED",
  "message": "Token inválido ou expirado"
}
```

### Erro 403 - Forbidden

```json
{
  "errorCode": "FORBIDDEN",
  "message": "Sem permissão para acessar este lojista"
}
```

### Erro 500 - Internal Server Error

```json
{
  "errorCode": "SETTLEMENT_SCHEDULE_ERROR",
  "message": "Erro ao consultar agenda de liquidação"
}
```

---

## Performance e Considerações

1. **Período Máximo:** Recomenda-se não consultar períodos maiores que 90 dias por vez
2. **Paginação:** O detalhe do dia retorna até 50 transações por padrão
3. **Cache:** Os dados são consultados em tempo real do banco de dados
4. **Autenticação:** Todos os endpoints requerem token JWT válido
5. **Autorização:** Um lojista só pode visualizar sua própria agenda

---

## Integração com Frontend

```javascript
// JavaScript/TypeScript
async function getSettlementSchedule(startDate, endDate, status = null) {
  const params = new URLSearchParams({
    startDate: startDate.toISOString().split('T')[0],
    endDate: endDate.toISOString().split('T')[0],
  });
  
  if (status) {
    params.append('status', status);
  }
  
  const response = await fetch(
    `/api/v1/settlements/schedule?${params}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  return response.json();
}

async function getSettlementDayDetail(settlementDate, status = null, page = 0, size = 50) {
  const params = new URLSearchParams({
    page: page.toString(),
    size: size.toString(),
  });
  
  if (status) {
    params.append('status', status);
  }
  
  const dateStr = settlementDate.toISOString().split('T')[0];
  const response = await fetch(
    `/api/v1/settlements/schedule/${dateStr}?${params}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  return response.json();
}
```


