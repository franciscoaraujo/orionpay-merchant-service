# OrionPay Merchant Service 🚀

Este é o microserviço central da plataforma de pagamentos **OrionPay**, responsável pela gestão de lojistas, processamento de transações, controle financeiro (Ledger) e liquidação.

Construído com **Java 21**, **Spring Boot 3.x** e seguindo os princípios de **Arquitetura Hexagonal (Ports & Adapters)** e **Domain-Driven Design (DDD)**.

---

## 🏗️ Arquitetura

O projeto segue uma arquitetura limpa, separando o núcleo de negócio das tecnologias externas.

### Estrutura de Pacotes
```
orionpay.merchant
├── application      # Camada de Aplicação (Ports - Interfaces de Entrada/Saída)
├── domain           # Núcleo de Negócio (Entidades, Value Objects, Use Cases)
├── infrastructure   # Implementações Técnicas (Adapters, Persistence, Controllers, Config)
└── config           # Configurações do Spring Framework
```

### Tecnologias Principais
*   **Java 21** (LTS)
*   **Spring Boot 3.x** (Web, Data JPA, Validation, Mail, Cache)
*   **PostgreSQL** (Banco de Dados Relacional)
*   **Redis** (Cache Distribuído)
*   **MapStruct** (Mapeamento de Objetos)
*   **Lombok** (Redução de Boilerplate)
*   **JUnit 5** (Testes)

---

## ✨ Funcionalidades e Otimizações

### 1. Onboarding de Lojistas (`/api/v1/merchants`)
*   Cadastro completo de lojista (Merchant).
*   Registro de endereço e conta bancária.
*   Criação automática da Conta Contábil (Ledger Account).

### 2. Autorização de Transações (`/api/v1/transactions`)
*   Processamento de transações de Débito e Crédito.
*   **Cache de Taxas (Redis)**: As regras de precificação (`MerchantPricing`) são cacheadas para reduzir latência e carga no banco durante autorizações.
*   Cálculo automático de taxas (MDR) e valor líquido.
*   **Ciclo de Liquidação**:
    *   **Débito**: Disponível em D+1.
    *   **Crédito**: Disponível em D+30.

### 3. Gestão Financeira (`/api/v1/dashboard`)
*   **Livro Razão (Ledger)**: Registro imutável de todas as movimentações financeiras (Partidas Dobradas).
*   **Dashboard Otimizado (Redis)**:
    *   Métricas pesadas (TPV, Gráficos) são cacheadas por 10 minutos.
    *   **Invalidação Inteligente**: O cache é limpo automaticamente a cada nova transação aprovada, garantindo dados sempre frescos ("near real-time").
    *   Saldo Disponível vs. Recebíveis Futuros.

### 4. Saque (Payout) (`/api/v1/withdrawals`)
*   Solicitação de saque via Pix.
*   **Trava de Segurança**: Validação estrita do Saldo Disponível (ignora recebíveis futuros).
*   Integração com Gateway de Pagamento (Mock/Simulação).

---

## 🚀 Como Rodar

### Pré-requisitos
*   Java 21 JDK
*   Maven
*   PostgreSQL (Local ou Docker)
*   Redis (Local ou Docker)

### 1. Configuração do Banco de Dados
Certifique-se de que o PostgreSQL está rodando. O `application.yml` está configurado para:
*   URL: `jdbc:postgresql://localhost:5432/orion_payments`
*   User: `postgres`
*   Pass: `admin`

*Dica: Execute o script `ddl_tables.sql` e `migration_settlement_cycle.sql` para criar a estrutura inicial.*

### 2. Configuração do Redis
Certifique-se de que o Redis está rodando na porta padrão `6379`.
```bash
docker run -d -p 6379:6379 redis
```

### 3. Configuração de E-mail (Opcional)
Para testar o envio de comprovantes, configure o SMTP no `application.yml` ou use o [MailHog](https://github.com/mailhog/MailHog) para simulação local.

### 4. Executando a Aplicação
```bash
mvn spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`.

---

## 📚 Documentação da API

### Criar Lojista
`POST /api/v1/merchants/onboarding`
```json
{
  "name": "Padaria do Chico",
  "document": "12345678000199",
  "email": "contato@padaria.com",
  "zipCode": "01310000",
  "street": "Av. Paulista",
  "number": "1000",
  "neighborhood": "Bela Vista",
  "city": "São Paulo",
  "state": "SP",
  "bankCode": "341",
  "branch": "1234",
  "account": "56789",
  "accountDigit": "0",
  "accountType": "CHECKING"
}
```

### Autorizar Transação
`POST /api/v1/transactions/authorize`
```json
{
  "merchantId": "UUID_DO_LOJISTA",
  "amount": 150.00,
  "productType": "CREDIT_A_VISTA",
  "terminalSerialNumber": "POS-001",
  "terminalSn": "POS-001",
  "entryMode": "CHIP"
}
```

### Solicitar Saque
`POST /api/v1/withdrawals`
```json
{
  "merchantId": "UUID_DO_LOJISTA",
  "amount": 50.00,
  "pixKey": "chave@pix.com"
}
```

### Consultar Dashboard
`GET /api/v1/dashboard/{merchantId}/summary`

---

## 🛡️ Segurança e Decisões Técnicas

*   **Ledger Imutável**: O saldo nunca é alterado diretamente sem um registro de `LedgerEntry` correspondente (Auditabilidade).
*   **Optimistic Locking**: Uso de `@Version` na entidade de conta para evitar condições de corrida em atualizações de saldo.
*   **UUIDs**: Utilizados como identificadores primários para segurança e escalabilidade.
*   **Cache Strategy**: Uso de Redis com TTL e Cache Eviction para equilibrar performance e consistência.
*   **Tratamento de Erros**: Exceções de domínio (`DomainException`) mapeadas para respostas HTTP adequadas.

---

## 📝 Licença

Este projeto é proprietário da **OrionPay**.
