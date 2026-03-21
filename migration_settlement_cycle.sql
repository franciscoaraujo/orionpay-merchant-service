-- Adiciona a coluna de data de disponibilidade para controle de liquidação
ALTER TABLE accounting.ledger_entry ADD COLUMN available_at TIMESTAMP;

-- Atualiza os registros antigos para estarem disponíveis imediatamente (migração de dados)
UPDATE accounting.ledger_entry SET available_at = created_at WHERE available_at IS NULL;

-- Define a coluna como NOT NULL após a migração
ALTER TABLE accounting.ledger_entry ALTER COLUMN available_at SET NOT NULL;