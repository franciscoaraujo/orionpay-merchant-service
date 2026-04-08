-- Script SQL para inserir dados de teste na tabela settlement_entry
-- Execute este script no banco de dados para testar a API

-- Merchant ID usado nos testes
-- 0f6ac19c-1bc2-43f8-a289-d0cf39615f02

-- Limpar dados existentes para este merchant (opcional)
-- DELETE FROM ops.settlement_entry WHERE merchant_id = '0f6ac19c-1bc2-43f8-a289-d0cf39615f02';

-- Inserir transações de teste para o período 2026-04-01 até 2026-04-30

-- Dia 1: 2026-04-05 - 3 transações SCHEDULED
INSERT INTO ops.settlement_entry (
  id, transaction_id, merchant_id, amount, net_amount,
  expected_settlement_date, status, created_at, updated_at,
  installment_number, total_installments, mdr_amount, mdr_percentage
) VALUES (
  gen_random_uuid(),
  gen_random_uuid(),
  '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid,
  100.00,
  97.00,
  '2026-04-05'::timestamp,
  'SCHEDULED',
  now(),
  now(),
  1,
  1,
  3.00,
  3.00
);

INSERT INTO ops.settlement_entry (
  id, transaction_id, merchant_id, amount, net_amount,
  expected_settlement_date, status, created_at, updated_at,
  installment_number, total_installments, mdr_amount, mdr_percentage
) VALUES (
  gen_random_uuid(),
  gen_random_uuid(),
  '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid,
  200.00,
  194.00,
  '2026-04-05'::timestamp,
  'SCHEDULED',
  now(),
  now(),
  1,
  1,
  6.00,
  3.00
);

INSERT INTO ops.settlement_entry (
  id, transaction_id, merchant_id, amount, net_amount,
  expected_settlement_date, status, created_at, updated_at,
  installment_number, total_installments, mdr_amount, mdr_percentage
) VALUES (
  gen_random_uuid(),
  gen_random_uuid(),
  '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid,
  150.00,
  145.50,
  '2026-04-05'::timestamp,
  'SCHEDULED',
  now(),
  now(),
  1,
  1,
  4.50,
  3.00
);

-- Dia 2: 2026-04-10 - 2 transações SETTLED
INSERT INTO ops.settlement_entry (
  id, transaction_id, merchant_id, amount, net_amount,
  expected_settlement_date, status, created_at, updated_at,
  installment_number, total_installments, mdr_amount, mdr_percentage
) VALUES (
  gen_random_uuid(),
  gen_random_uuid(),
  '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid,
  300.00,
  291.00,
  '2026-04-10'::timestamp,
  'SETTLED',
  now(),
  now(),
  1,
  1,
  9.00,
  3.00
);

INSERT INTO ops.settlement_entry (
  id, transaction_id, merchant_id, amount, net_amount,
  expected_settlement_date, status, created_at, updated_at,
  installment_number, total_installments, mdr_amount, mdr_percentage
) VALUES (
  gen_random_uuid(),
  gen_random_uuid(),
  '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid,
  250.00,
  242.50,
  '2026-04-10'::timestamp,
  'SETTLED',
  now(),
  now(),
  1,
  1,
  7.50,
  3.00
);

-- Dia 3: 2026-04-15 - 1 transação PENDING
INSERT INTO ops.settlement_entry (
  id, transaction_id, merchant_id, amount, net_amount,
  expected_settlement_date, status, created_at, updated_at,
  installment_number, total_installments, mdr_amount, mdr_percentage
) VALUES (
  gen_random_uuid(),
  gen_random_uuid(),
  '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid,
  400.00,
  388.00,
  '2026-04-15'::timestamp,
  'PENDING',
  now(),
  now(),
  1,
  1,
  12.00,
  3.00
);

-- Verificar dados inseridos
SELECT
    CAST(expected_settlement_date AS DATE) as date,
    COUNT(*) as transactions,
    SUM(amount) as total_gross,
    SUM(net_amount) as total_net,
    STRING_AGG(DISTINCT status, ', ') as statuses
FROM ops.settlement_entry
WHERE merchant_id = '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid
AND expected_settlement_date >= '2026-04-01'::date
AND expected_settlement_date <= '2026-04-30'::date
GROUP BY CAST(expected_settlement_date AS DATE)
ORDER BY date;

-- Contagem total
SELECT
    COUNT(*) as total_transactions,
    SUM(amount) as total_gross,
    SUM(net_amount) as total_net
FROM ops.settlement_entry
WHERE merchant_id = '0f6ac19c-1bc2-43f8-a289-d0cf39615f02'::uuid
AND expected_settlement_date >= '2026-04-01'::date
AND expected_settlement_date <= '2026-04-30'::date;
 u