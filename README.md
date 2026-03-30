# OrionPay Merchant Service 🚀

Este é o microserviço central da plataforma de pagamentos **OrionPay**, responsável pela gestão de lojistas, processamento de transações, controle financeiro (Ledger), liquidação e resiliência.

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
*   **Spring Boot 3.x** (Web, Data JPA, Validation, AMQP, Cache)
*   **RabbitMQ** (Mensageria e Motor de Eventos)
*   **PostgreSQL** (Banco de Dados Relacional)
*   **Redis** (Cache Distribuído e Idempotência)
*   **Resilience4j** (Circuit Breaker, Retry e Bulkhead)
*   **MapStruct** (Mapeamento de Objetos)
*   **Lombok** (Redução de Boilerplate)

---

## ✨ Funcionalidades e Otimizações de Elite

### 1. Motor de Liquidação Assíncrono (Settlement Engine)
*   **Arquitetura Baseada em Eventos**: Transações autorizadas disparam eventos via **RabbitMQ** para processamento financeiro desacoplado.
*   **Garantia de Idempotência**: Uso de travas distribuídas no **Redis** para evitar duplicidade de liquidação.
*   **Resiliência Financeira**: Implementação de **Dead Letter Queues (DLQ)** para garantir que nenhuma falha de processamento resulte em perda de dados contábeis.

### 2. Resiliência e Tolerância a Falhas
*   **Circuit Breaker & Retry**: Proteção contra falhas no Gateway de Adquirentes e serviços externos usando **Resilience4j**.
*   **Bulkhead Isolation**: Limitação de execução concorrente no motor de liquidação para proteger a saúde do sistema em picos de carga.
*   **Fallback Logic**: Respostas tratadas ("Sistema Indisponível") em vez de erros 500 durante instabilidades externas.

### 3. Inteligência Financeira e Performance (Sprint 1 - Read Models)
*   **Dashboard de Alta Performance**: Implementação de **Read Models** (Tabelas de Resumo Diário) com complexidade de leitura **O(1)**.
*   **Upsert Atômico**: Uso de `ON CONFLICT DO UPDATE` no PostgreSQL para consolidação de métricas em tempo real (TPV, Net Revenue, Ticket Médio).
*   **Comparação de Período Equivalente**: Inteligência de BI que compara "Hoje vs Ontem" no mesmo intervalo de horas para métricas precisas.

### 4. Segurança e Auditoria
*   **Idempotência em API**: Header `X-Idempotency-Key` obrigatório em fluxos críticos (Saque e Autorização).
*   **JWT com JTI**: Tokens de acesso e refresh protegidos com Identificador Único (JTI) para prevenir ataques de replay.
*   **Ledger Imutável (Livro Razão)**: Registro rigoroso de todas as movimentações financeiras seguindo o padrão de partidas dobradas.

---

## 🚀 Como Rodar

### Pré-requisitos
*   Java 21 JDK
*   Maven
*   PostgreSQL (Local ou Docker)
*   Redis (Local ou Docker)
*   RabbitMQ (Local ou Docker)

### 1. Configuração da Infraestrutura (Docker)
```bash
# Subir Redis e RabbitMQ rapidamente
docker run -d --name redis -p 6379:6379 redis
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 2. Configuração do Banco de Dados
O `application.yml` está configurado para:
*   URL: `jdbc:postgresql://localhost:5432/orion_payments`
*   User: `postgres`
*   Pass: `admin`

*Dica: Certifique-se de que o schema `ops` e `accounting` existam no seu banco.*

### 3. Executando a Aplicação
```bash
mvn spring-boot:run
```

---

## 📚 Endpoints Principais

*   **Autorizar Transação**: `POST /api/v1/transactions/authorize` (Requer `X-Idempotency-Key`)
*   **Dashboard Summary**: `GET /api/v1/dashboard/{merchantId}/summary`
*   **Solicitar Saque**: `POST /api/v1/withdrawals` (Requer `X-Idempotency-Key`)
*   **Extrato de Transações**: `GET /api/v1/transactions/{merchantId}/extrato`

---

## 🛡️ Decisões Técnicas de Escala
*   **Separação de Preocupações**: A lógica de autorização (rápida) é separada da lógica de liquidação (lenta/complexa) via mensageria.
*   **Escalabilidade de Leitura**: O Dashboard lê resumos pré-calculados, permitindo que o sistema suporte milhões de usuários simultâneos sem degradar a performance do banco.

---

## 📝 Licença
Este projeto é proprietário da **OrionPay**.
