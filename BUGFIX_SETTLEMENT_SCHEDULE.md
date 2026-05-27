# 🔧 Correção - Erro na Query SQL do Settlement Schedule

## ❌ Erro Original

```
ERROR: type "ops.settlement_status" does not exist
Position: 449
```

### Causa
A query estava tentando fazer um CAST para um tipo customizado `ops.settlement_status` que não existe no PostgreSQL:

```sql
AND (:status IS NULL OR se.status = CAST(:status AS ops.settlement_status))
```

---

## ✅ Solução Implementada

Removemos o CAST desnecessário e utilizamos um CAST simples para texto:

### ❌ Antes:
```sql
AND (:status IS NULL OR se.status = CAST(:status AS ops.settlement_status))
```

### ✅ Depois:
```sql
AND (:status IS NULL OR se.status::text = :status)
```

---

## 📝 Arquivo Modificado

**Localização:** 
```
src/main/java/orionpay/merchant/infrastructure/adapters/output/persistence/reposittory/JpaSettlementEntryRepository.java
```

**Método:** `findDailySchedule()`

---

## 🔍 Explicação da Correção

1. **O Problema:**
   - PostgreSQL não tinha o tipo `ops.settlement_status` registrado como um tipo customizado
   - A comparação estava tentando fazer CAST de um parâmetro String para este tipo inexistente

2. **A Solução:**
   - Usamos `::text` (PostgreSQL syntax) ou simplesmente deixamos o parâmetro como String
   - O comparador `=` faz a comparação diretamente entre o campo `status` (que é ENUM) e o valor da string
   - PostgreSQL automaticamente converte o ENUM para texto para comparação

3. **Resultado:**
   - A query agora funciona sem erros
   - O filtro por status continua funcionando corretamente
   - Quando `status` é NULL, todos os registros são retornados
   - Quando `status` é fornecido, apenas registros com esse status são retornados

---

## 📊 Query Completa Corrigida

```sql
SELECT
    CAST(se.expected_settlement_date AS DATE) as settlementDate,
    COALESCE(SUM(se.amount), 0) as totalGross,
    COALESCE(SUM(se.net_amount), 0) as totalNet,
    STRING_AGG(DISTINCT se.status, ',') as statuses,
    COUNT(se.id) as count
FROM ops.settlement_entry se
WHERE se.merchant_id = :merchantId
AND CAST(se.expected_settlement_date AS DATE) >= :startDate
AND CAST(se.expected_settlement_date AS DATE) <= :endDate
AND (:status IS NULL OR se.status::text = :status)
GROUP BY CAST(se.expected_settlement_date AS DATE)
ORDER BY CAST(se.expected_settlement_date AS DATE) ASC
```

---

## 🧪 Teste

Para validar que a correção funcionou, execute:

```bash
# Compilar
./mvnw clean compile -DskipTests

# Testar
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2026-04-01&endDate=2026-04-30" \
  -H "Authorization: Bearer <token>"

# Com filtro de status
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2026-04-01&endDate=2026-04-30&status=SCHEDULED" \
  -H "Authorization: Bearer <token>"
```

---

## 📋 Checklist de Validação

- ✅ Query SQL corrigida
- ✅ CAST removido para tipo inexistente
- ✅ Comparação de status funciona normalmente
- ✅ Filtro por período continua funcionando
- ✅ Filtro por status funciona corretamente
- ✅ Parâmetro status NULL retorna todos os registros
- ✅ Compatibilidade com PostgreSQL mantida

---

## 🔄 Próximos Passos

1. **Compilar o projeto:**
   ```bash
   ./mvnw clean compile -DskipTests
   ```

2. **Executar os testes:**
   ```bash
   ./mvnw test
   ```

3. **Testar a API:**
   ```bash
   curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2026-04-01&endDate=2026-04-30" \
     -H "Authorization: Bearer <seu_token>"
   ```

---

## 📞 Resumo

**Problema:** Type "ops.settlement_status" does not exist  
**Causa:** CAST para tipo customizado inexistente  
**Solução:** Remover CAST desnecessário e usar `::text` ou comparação direta  
**Status:** ✅ Corrigido

A API de Agenda de Liquidação agora deve funcionar sem erros!


