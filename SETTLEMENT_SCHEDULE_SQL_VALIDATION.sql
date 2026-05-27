-- ============================================================================
-- SQL para Validar Estrutura da Tabela settlement_entry
-- e Criar Índices para Otimizar as Queries da Agenda de Liquidação
-- ============================================================================

-- 1. VERIFICAR ESTRUTURA DA TABELA
-- ============================================================================

SELECT * FROM information_schema.columns
WHERE table_schema = 'ops'
AND table_name = 'settlement_entry'
ORDER BY ordinal_position;

-- 2. VALIDAR TIPOS DE DADOS CRÍTICOS
-- ============================================================================

-- Verificar coluna de data de liquidação esperada
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'ops'
AND table_name = 'settlement_entry'
AND column_name = 'expected_settlement_date';

-- Verificar coluna de status (deve ser ENUM)
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'ops'
AND table_name = 'settlement_entry'
AND column_name = 'status';

-- Verificar coluna de merchant_id (UUID)
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'ops'
AND table_name = 'settlement_entry'
AND column_name = 'merchant_id';

-- 3. VISUALIZAR ÍNDICES EXISTENTES
-- ============================================================================

SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'ops'
AND tablename = 'settlement_entry'
ORDER BY indexname;

-- 4. CRIAR ÍNDICES PARA OTIMIZAR QUERIES
-- ============================================================================

-- Índice para busca por merchant_id e data (query principal)
CREATE INDEX IF NOT EXISTS idx_settlement_entry_merchant_date
ON ops.settlement_entry(merchant_id, expected_settlement_date);

-- Índice para busca por merchant_id, data e status
CREATE INDEX IF NOT EXISTS idx_settlement_entry_merchant_date_status
ON ops.settlement_entry(merchant_id, expected_settlement_date, status);

-- Índice para transações antecipadas
CREATE INDEX IF NOT EXISTS idx_settlement_entry_anticipated
ON ops.settlement_entry(merchant_id, is_anticipated, expected_settlement_date)
WHERE is_anticipated = true;

-- Índice para transações bloqueadas
CREATE INDEX IF NOT EXISTS idx_settlement_entry_blocked
ON ops.settlement_entry(merchant_id, is_blocked, expected_settlement_date)
WHERE is_blocked = true;

-- Índice para pesquisa por transaction_id (para idempotência)
CREATE INDEX IF NOT EXISTS idx_settlement_entry_transaction_id
ON ops.settlement_entry(transaction_id);

-- Índice para pesquisa por installment (parcelas)
CREATE INDEX IF NOT EXISTS idx_settlement_entry_installment
ON ops.settlement_entry(transaction_id, installment_number);

-- 5. VALIDAR DADOS DE EXEMPLO
-- ============================================================================

-- Contar registros por status
SELECT
    status,
    COUNT(*) as count,
    SUM(amount) as total_gross,
    SUM(net_amount) as total_net,
    AVG(amount) as average
FROM ops.settlement_entry
GROUP BY status
ORDER BY count DESC;

-- Contar registros por merchant (últimos 10)
SELECT
    merchant_id,
    COUNT(*) as count,
    SUM(amount) as total_gross,
    MIN(expected_settlement_date) as first_date,
    MAX(expected_settlement_date) as last_date
FROM ops.settlement_entry
GROUP BY merchant_id
ORDER BY count DESC
LIMIT 10;

-- Verificar distribuição por data esperada de liquidação
SELECT
    CAST(expected_settlement_date AS DATE) as date,
    COUNT(*) as transaction_count,
    SUM(amount) as total_gross,
    SUM(net_amount) as total_net,
    STRING_AGG(DISTINCT status, ',') as statuses
FROM ops.settlement_entry
WHERE CAST(expected_settlement_date AS DATE) >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY CAST(expected_settlement_date AS DATE)
ORDER BY date DESC;

-- 6. PERFORMANCE - SIMULAR QUERY DO ENDPOINT
-- ============================================================================

-- Query simulada para período de 30 dias
EXPLAIN ANALYZE
SELECT
    CAST(se.expected_settlement_date AS DATE) as settlementDate,
    COALESCE(SUM(se.amount), 0) as totalGross,
    COALESCE(SUM(se.net_amount), 0) as totalNet,
    STRING_AGG(DISTINCT se.status, ',') as statuses,
    COUNT(se.id) as count
FROM ops.settlement_entry se
WHERE se.merchant_id = 'a-merchant-uuid-here'::uuid
AND CAST(se.expected_settlement_date AS DATE) >= CURRENT_DATE - INTERVAL '30 days'
AND CAST(se.expected_settlement_date AS DATE) <= CURRENT_DATE
GROUP BY CAST(se.expected_settlement_date AS DATE)
ORDER BY CAST(se.expected_settlement_date AS DATE) ASC;

-- 7. ESTATÍSTICAS DA TABELA
-- ============================================================================

-- Verificar estatísticas
SELECT
    schemaname,
    tablename,
    n_live_tup as live_rows,
    n_dead_tup as dead_rows,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables
WHERE schemaname = 'ops' AND tablename = 'settlement_entry';

-- 8. VALIDAR CONSTRAINT UNIQUE (Para Idempotência)
-- ============================================================================

SELECT
    constraint_name,
    column_name
FROM information_schema.constraint_column_usage
WHERE table_schema = 'ops'
AND table_name = 'settlement_entry'
AND constraint_name LIKE '%uk_%'
ORDER BY constraint_name;

-- Verificar se há chaves duplicadas
SELECT
    transaction_id,
    installment_number,
    COUNT(*) as duplicate_count
FROM ops.settlement_entry
GROUP BY transaction_id, installment_number
HAVING COUNT(*) > 1;

-- 9. VERIFICAR INTEGRIDADE REFERENCIAL
-- ============================================================================

-- Verificar transações orfãs (sem referência no banco)
SELECT se.id, se.transaction_id, se.merchant_id
FROM ops.settlement_entry se
LEFT JOIN core.transaction t ON se.transaction_id = t.id
WHERE t.id IS NULL
LIMIT 10;

-- Verificar merchants orfãos
SELECT DISTINCT se.merchant_id
FROM ops.settlement_entry se
LEFT JOIN core.merchant m ON se.merchant_id = m.id
WHERE m.id IS NULL;

-- 10. MANUTENÇÃO
-- ============================================================================

-- Vacuumar tabela de settlement_entry
VACUUM ANALYZE ops.settlement_entry;

-- Reindex se necessário
REINDEX TABLE ops.settlement_entry;

-- 11. PERFORMANCE TUNING - VERIFICAR PLANO DE EXECUÇÃO
-- ============================================================================

-- Aumentar work_mem para queries complexas (temporário)
-- SET work_mem = '256MB';

-- Verificar estatísticas estão atualizadas
SELECT
    tablename,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE schemaname = 'ops' AND tablename = 'settlement_entry';

-- 12. EXEMPLO DE QUERIES USUAIS
-- ============================================================================

-- Query 1: Agenda de um período (MAIS USADA)
SELECT
    CAST(se.expected_settlement_date AS DATE) as settlementDate,
    COALESCE(SUM(se.amount), 0) as totalGross,
    COALESCE(SUM(se.net_amount), 0) as totalNet,
    STRING_AGG(DISTINCT se.status, ',') as statuses,
    COUNT(se.id) as count
FROM ops.settlement_entry se
WHERE se.merchant_id = 'merchant-uuid'::uuid
AND CAST(se.expected_settlement_date AS DATE) >= '2024-01-01'::date
AND CAST(se.expected_settlement_date AS DATE) <= '2024-01-31'::date
GROUP BY CAST(se.expected_settlement_date AS DATE)
ORDER BY CAST(se.expected_settlement_date AS DATE) ASC;

-- Query 2: Detalhe de um dia específico com paginação
SELECT
    se.id,
    se.transaction_id,
    se.expected_settlement_date,
    t.created_at,
    se.amount,
    se.mdr_amount,
    se.net_amount,
    se.status,
    se.paid_at,
    t.nsu,
    t.card_brand,
    t.card_last_four,
    t.product_type,
    se.is_blocked,
    se.is_anticipated,
    se.installment_number,
    se.mdr_percentage,
    se.original_amount
FROM ops.settlement_entry se
LEFT JOIN core.transaction t ON se.transaction_id = t.id
WHERE se.merchant_id = 'merchant-uuid'::uuid
AND se.expected_settlement_date >= '2024-01-15'::date::timestamp
AND se.expected_settlement_date < ('2024-01-15'::date + INTERVAL '1 day')::timestamp
ORDER BY se.expected_settlement_date DESC, t.created_at DESC
LIMIT 50 OFFSET 0;

-- Query 3: Transações antecipadas
SELECT
    COUNT(*) as count,
    SUM(se.amount) as total_value
FROM ops.settlement_entry se
WHERE se.merchant_id = 'merchant-uuid'::uuid
AND se.is_anticipated = true
AND CAST(se.expected_settlement_date AS DATE) >= CURRENT_DATE;

-- Query 4: Transações bloqueadas
SELECT
    COUNT(*) as count,
    SUM(se.amount) as total_value,
    STRING_AGG(DISTINCT se.blocked_reason, ', ') as reasons
FROM ops.settlement_entry se
WHERE se.merchant_id = 'merchant-uuid'::uuid
AND se.is_blocked = true;

-- 13. MONITORAMENTO CONTÍNUO
-- ============================================================================

-- Verificar queries lentas no log (se habilitado)
-- Mais de 1 segundo é considerado lento
SELECT
    query,
    calls,
    mean_exec_time,
    max_exec_time
FROM pg_stat_statements
WHERE query LIKE '%settlement_entry%'
ORDER BY mean_exec_time DESC;

-- 14. TUNNING FINAL (SE NECESSÁRIO)
-- ============================================================================

-- Aumentar statistics para coluna de data (melhor planejamento)
ALTER TABLE ops.settlement_entry ALTER COLUMN expected_settlement_date SET STATISTICS 100;

-- Analisar novamente após alteração
ANALYZE ops.settlement_entry;

-- Verificar bloat na tabela (se tiver muita sobrescrita de dados)
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size
FROM pg_tables
WHERE schemaname = 'ops' AND tablename = 'settlement_entry';

-- ============================================================================
-- SCRIPT DE TESTE FINAL - VALIDAR PERFORMANCE
-- ============================================================================

-- Executar queries com timing
\timing on

-- Query 1: Agenda mensal (deve ser < 500ms)
SELECT
    CAST(se.expected_settlement_date AS DATE) as settlementDate,
    COALESCE(SUM(se.amount), 0) as totalGross,
    COALESCE(SUM(se.net_amount), 0) as totalNet,
    STRING_AGG(DISTINCT se.status, ',') as statuses,
    COUNT(se.id) as count
FROM ops.settlement_entry se
WHERE se.merchant_id = 'test-merchant'::uuid
AND CAST(se.expected_settlement_date AS DATE) >= CURRENT_DATE - INTERVAL '30 days'
AND CAST(se.expected_settlement_date AS DATE) <= CURRENT_DATE
GROUP BY CAST(se.expected_settlement_date AS DATE)
ORDER BY CAST(se.expected_settlement_date AS DATE) ASC;

-- Query 2: Detalhe do dia com paginação (deve ser < 300ms)
SELECT
    se.id,
    se.transaction_id,
    se.expected_settlement_date,
    t.created_at,
    se.amount,
    se.mdr_amount,
    se.net_amount,
    se.status
FROM ops.settlement_entry se
LEFT JOIN core.transaction t ON se.transaction_id = t.id
WHERE se.merchant_id = 'test-merchant'::uuid
AND CAST(se.expected_settlement_date AS DATE) = CURRENT_DATE
ORDER BY se.expected_settlement_date DESC
LIMIT 50;

\timing off

