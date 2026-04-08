# 🏗️ Arquitetura - Settlement Schedule

## Fluxo de Requisição

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            FRONTEND (React/Angular)                         │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ HTTP GET
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          REST CONTROLLER                                     │
│  SettlementController                                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @GetMapping("/schedule")                                            │   │
│  │ getSchedule(startDate, endDate, status)                             │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ @GetMapping("/schedule/{settlementDate}")                           │   │
│  │ getScheduleDayDetail(settlementDate, status, page, size)            │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    ▼                                   ▼
┌───────────────────────────────────┐  ┌──────────────────────────────────────┐
│         USE CASES (Domain)        │  │      USE CASES (Domain)              │
│                                   │  │                                      │
│ GetSettlementScheduleUseCase      │  │ GetSettlementDayDetailUseCase       │
│ ┌─────────────────────────────┐   │  │ ┌──────────────────────────────┐    │
│ │ execute(merchantId,         │   │  │ │ execute(merchantId,          │    │
│ │         startDate,          │   │  │ │         settlementDate,      │    │
│ │         endDate,            │   │  │ │         status,              │    │
│ │         status)             │   │  │ │         pageable)            │    │
│ └─────────────────────────────┘   │  │ └──────────────────────────────┘    │
│                                   │  │                                      │
│ • Calcula agregações             │  │ • Calcula agregações por dia        │
│ • Soma totalizadores             │  │ • Mapeia transações detalhadas      │
│ • Formata respostas              │  │ • Suporta paginação                 │
└───────────────────────────────────┘  └──────────────────────────────────────┘
                    │                                   │
                    └─────────────────┬─────────────────┘
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    REPOSITORY (Persistence Layer)                           │
│                                                                              │
│ JpaSettlementEntryRepository                                               │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ findDailySchedule(merchantId, startDate, endDate, status)              │ │
│ │ • Query SQL com GROUP BY e agregações                                 │ │
│ │ • Retorna DailyScheduleProjection[]                                   │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ findAgendaByPeriod(merchantId, start, end, status, pageable)           │ │
│ │ • Query com LEFT JOIN com tabela transaction                           │ │
│ │ • Retorna Page<AgendaItemProjection>                                  │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         DATABASE (PostgreSQL)                               │
│                                                                              │
│ ops.settlement_entry (tabela principal)                                    │
│ ├── id (UUID, PK)                                                          │
│ ├── transaction_id (UUID, FK)                                              │
│ ├── merchant_id (UUID)                                                     │
│ ├── expected_settlement_date (TIMESTAMP) ◄──── AGRUPADO                   │
│ ├── amount (DECIMAL) ◄────────────────── SOMADO                          │
│ ├── net_amount (DECIMAL) ◄──────────────── SOMADO                        │
│ ├── status (ENUM) ◄───────────────────── AGREGADO                        │
│ ├── is_blocked (BOOLEAN)                                                  │
│ ├── is_anticipated (BOOLEAN)                                              │
│ ├── mdr_amount (DECIMAL)                                                  │
│ ├── mdr_percentage (DECIMAL)                                              │
│ ├── installment_number (INTEGER)                                          │
│ └── ... mais campos                                                        │
│                                                                              │
│ core.transaction (tabela relacionada)                                      │
│ ├── id (UUID, PK)                                                          │
│ ├── nsu (VARCHAR)                                                          │
│ ├── card_brand (VARCHAR)                                                   │
│ ├── card_last_four (VARCHAR)                                               │
│ ├── created_at (TIMESTAMP)                                                 │
│ └── ... mais campos                                                        │
│                                                                              │
│ ÍNDICES CRIADOS:                                                           │
│ ├── idx_settlement_entry_merchant_date                                     │
│ ├── idx_settlement_entry_merchant_date_status                              │
│ ├── idx_settlement_entry_anticipated                                       │
│ ├── idx_settlement_entry_blocked                                           │
│ └── idx_settlement_entry_transaction_id                                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Estrutura de Dados

### Response 1: Agenda Geral (SettlementScheduleResponse)

```
SettlementScheduleResponse
├── periodStart: LocalDate                    // "2024-01-01"
├── periodEnd: LocalDate                      // "2024-01-31"
├── totalPeriodGross: BigDecimal              // "50000.00"
├── totalPeriodNet: BigDecimal                // "48500.00"
├── totalTransactionsInPeriod: Integer        // 215
└── schedule: List<DailySchedule>
    └── DailySchedule[]
        ├── date: LocalDate                   // "2024-01-05"
        ├── totalGross: BigDecimal            // "2500.00"
        ├── totalNet: BigDecimal              // "2425.00"
        ├── mdrAmount: BigDecimal             // "75.00"
        ├── statusSummary: Set<String>        // ["SCHEDULED", "PENDING"]
        ├── transactionCount: Integer         // 12
        └── dailyAverageTransaction: BigDecimal // "208.33"
```

### Response 2: Detalhe do Dia (SettlementDayDetailResponse)

```
SettlementDayDetailResponse
├── settlementDate: LocalDate                 // "2024-01-15"
├── totalGross: BigDecimal                    // "3500.00"
├── totalMdr: BigDecimal                      // "105.00"
├── totalNet: BigDecimal                      // "3395.00"
├── averageTransaction: BigDecimal            // "194.44"
├── totalCount: Integer                       // 18
├── blockedCount: Integer                     // 1
├── anticipatedCount: Integer                 // 2
├── statusBreakdown: Map<String, Long>        // {"SCHEDULED": 15, "BLOCKED": 1, ...}
├── transactions: List<TransactionDetail>
│   └── TransactionDetail[]
│       ├── idExt: UUID
│       ├── transactionId: UUID
│       ├── nsu: String                       // "123456"
│       ├── transactionDate: LocalDateTime
│       ├── settlementDate: LocalDateTime
│       ├── paidAt: LocalDateTime
│       ├── grossAmount: BigDecimal
│       ├── originalAmount: BigDecimal
│       ├── mdrPercentage: BigDecimal
│       ├── mdrAmount: BigDecimal
│       ├── netAmount: BigDecimal
│       ├── cardBrand: String                 // "VISA"
│       ├── cardLastFour: String              // "1234"
│       ├── productType: String               // "CREDIT_CARD"
│       ├── blocked: Boolean
│       ├── anticipated: Boolean
│       ├── installmentNumber: Integer
│       ├── installmentLabel: String          // "1/3"
│       └── status: String                    // "SCHEDULED"
├── pageNumber: Integer                       // 0
├── pageSize: Integer                         // 50
├── totalPages: Integer                       // 1
└── totalElements: Long                       // 18
```

---

## Fluxo de Dados - Query SQL

```
SQL Query
│
├─ WHERE merchant_id = ?
│  └─ Filtra por lojista (segurança)
│
├─ WHERE expected_settlement_date >= startDate
│  └─ Data mínima do período
│
├─ WHERE expected_settlement_date <= endDate
│  └─ Data máxima do período
│
├─ WHERE status = ? (opcional)
│  └─ Filtro por status específico
│
├─ GROUP BY expected_settlement_date
│  └─ Agrupa por data (chave principal)
│
├─ SELECT CAST(...AS DATE) as settlementDate
│  └─ Data de liquidação esperada
│
├─ SELECT SUM(amount) as totalGross
│  └─ Soma de valores brutos
│
├─ SELECT SUM(net_amount) as totalNet
│  └─ Soma de valores líquidos
│
├─ SELECT STRING_AGG(DISTINCT status, ',') as statuses
│  └─ Agregação de status presentes
│
└─ SELECT COUNT(*) as count
   └─ Contagem de registros por dia

Resultado
│
├─ Retorna Projection para cada dia
│  └─ DailyScheduleProjection
│      ├─ date (agrupador)
│      ├─ totalGross (agregado)
│      ├─ totalNet (agregado)
│      ├─ statuses (agregado)
│      └─ count (agregado)
│
├─ Use Case mapeia para DTO
│  └─ Calcula MDR amount e ticket médio
│
└─ Controller retorna Response JSON
```

---

## Índices de Performance

### Índice 1: Busca Principal (Agenda)

```sql
CREATE INDEX idx_settlement_entry_merchant_date 
ON ops.settlement_entry(merchant_id, expected_settlement_date);

Benefício: Filtro rápido por merchant + data
Cobertura: 90% das queries
```

### Índice 2: Busca com Status

```sql
CREATE INDEX idx_settlement_entry_merchant_date_status 
ON ops.settlement_entry(merchant_id, expected_settlement_date, status);

Benefício: Filtro rápido por merchant + data + status
Cobertura: 5% das queries (filtradas)
```

### Índice 3: Transações Antecipadas

```sql
CREATE INDEX idx_settlement_entry_anticipated 
ON ops.settlement_entry(merchant_id, is_anticipated, expected_settlement_date)
WHERE is_anticipated = true;

Benefício: Busca rápida de transações antecipadas
Tamanho reduzido: Apenas registros com is_anticipated = true
```

### Índice 4: Transações Bloqueadas

```sql
CREATE INDEX idx_settlement_entry_blocked 
ON ops.settlement_entry(merchant_id, is_blocked, expected_settlement_date)
WHERE is_blocked = true;

Benefício: Busca rápida de transações bloqueadas
Tamanho reduzido: Apenas registros com is_blocked = true
```

---

## Fluxo de Segurança

```
┌─ Requisição HTTP
   │
   ├─ JWT Token validado
   │  └─ Se inválido: 401 Unauthorized
   │
   ├─ SecurityContextService.getCurrentMerchantId()
   │  └─ Extrai merchant_id do token
   │
   ├─ Use Case executa com merchant_id
   │  └─ Query filtrada por este merchant
   │
   └─ Response retorna apenas dados do merchant
      └─ Um lojista NUNCA vê dados de outro
```

---

## Validações de Entrada

```
┌─ GET /api/v1/settlements/schedule?startDate=X&endDate=Y&status=Z
   │
   ├─ startDate NOT NULL ✓
   │  └─ Erro se nulo: 400 Bad Request
   │
   ├─ endDate NOT NULL ✓
   │  └─ Erro se nulo: 400 Bad Request
   │
   ├─ startDate <= endDate ✓
   │  └─ Erro se startDate > endDate: 400 Bad Request
   │
   ├─ status IN ['PENDING', 'SCHEDULED', ..., 'PREPAID'] (opcional)
   │  └─ Query a aceita qualquer string (validação no DB)
   │
   └─ merchantId extraído do token ✓
      └─ Erro se não autenticado: 401 Unauthorized
```

---

## Tipos de Status e Fluxos

```
                    CRIAÇÃO
                       │
                       ▼
                    PENDING
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
      SCHEDULED    BLOCKED      FAILED
         │             │
         │             └─► DISPUTE
         │                  │
         ├────────────────┬─┘
         │                │
         ▼                ▼
      SETTLED ◄─── ANTICIPATED
         │
         ▼
        PAID
```

---

## Performance esperada

```
Query: Agenda de 30 dias
│
├─ Sem índice: ~2000ms (lento)
├─ Com índice merchant_date: ~200ms (bom)
└─ Com pré-agregação: ~50ms (ótimo)

Query: Detalhe do dia (50 registros)
│
├─ Sem índice: ~500ms
├─ Com índice merchant_date: ~50ms
└─ Com índice + paginação: ~20ms

SLA Recomendado:
├─ Agenda: < 500ms (P95)
└─ Detalhe: < 300ms (P95)
```

---

## Stack Tecnológico

```
┌──────────────────────────────────┐
│   Frontend                       │
│  (React/Angular/Vue)             │
└──────────────────────────────────┘
            │ HTTP
            ▼
┌──────────────────────────────────┐
│   Spring Boot 3.x                │
│   REST Controller                │
│   @GetMapping                    │
└──────────────────────────────────┘
            │
            ▼
┌──────────────────────────────────┐
│   Spring Data JPA                │
│   JpaRepository                  │
│   Query Methods                  │
└──────────────────────────────────┘
            │ SQL
            ▼
┌──────────────────────────────────┐
│   PostgreSQL 12+                 │
│   Native Queries                 │
│   GROUP BY, SUM(), COUNT()       │
└──────────────────────────────────┘
```


