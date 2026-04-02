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

---

## ✨ Funcionalidades e Otimizações de Elite

### 1. Pattern Transactional Outbox (Consistência Eventual Garantida) 🛡️
Para garantir que nenhuma venda aprovada deixe de ser liquidada, implementamos o padrão **Transactional Outbox**.
*   **Problema Resolvido**: Evita a perda de mensagens quando o banco de dados commita a transação mas o broker de mensageria (RabbitMQ) falha.
*   **Funcionamento**: A venda e o evento de liquidação são salvos na **mesma transação** do banco de dados (Tabela `core.outbox`). Um componente **Outbox Relay** assíncrono publica no RabbitMQ com garantia **At-Least-Once**.

### 2. Idempotência Lógica e Mecanismo de Cura (Check-and-Skip) 🔄
Implementamos uma estratégia de idempotência de camada de aplicação para máxima integridade.
*   **Check-and-Skip**: O motor de liquidação consulta o estado da parcela antes de processar. Se já estiver em estado final (`SCHEDULED`, `PAID`, etc.), a operação é ignorada para evitar duplicidade.
*   **Mecanismo de Cura**: Caso uma parcela exista no status `PENDING` (devido a falhas anteriores no Ledger), o motor tenta completar o processamento contábil para o registro existente, em vez de criar um novo.

### 3. Motor de Liquidação e Orquestração de Parcelas 💳
*   **Explosão de Parcelas**: Transações de Crédito Parcelado são decompostas em múltiplos recebíveis na tabela `ops.settlement_entry`.
*   **Resiliência Granular**: Uso de `Propagation.REQUIRES_NEW` para persistir o estado `PENDING` imediatamente, garantindo auditabilidade mesmo se o módulo contábil falhar.

### 4. Antecipação de Recebíveis com Precisão Bancária 💰
*   **Simulação e Execução**: Cálculo de custo de antecipação pro-rata baseado na taxa real do lojista e dias restantes para liquidação (D+N).
*   **Liquidez Imediata**: Uso de `LocalDate` para precisão bancária e crédito instantâneo no saldo disponível via Ledger.

### 5. Dashboard de Alta Performance (Read Models) 📊
*   **Métricas em Tempo Real**: Tabelas de resumo diário (`daily_merchant_summary`) com complexidade de leitura **O(1)**.
*   **Inteligência de BI**: Filtros dinâmicos para *Hoje, Ontem, Mês Atual e Últimos 30 Dias* com comparativos de performance.

---

## 🛠️ Tecnologias Principais
*   **Java 21** & **Spring Boot 3.x**
*   **RabbitMQ**: Mensageria e eventos.
*   **PostgreSQL**: Banco de dados relacional.
*   **Redis**: Cache e locks de idempotência.
*   **MapStruct**: Mapeamento de objetos.

---

## 🚀 Como Rodar

### Pré-requisitos
*   Java 21 JDK
*   Maven
*   Infraestrutura (PostgreSQL, Redis, RabbitMQ)

### Executando a Aplicação
```bash
mvn spring-boot:run
```

---

## 📝 Licença
Este projeto é proprietário da **OrionPay**.
