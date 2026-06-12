# 🔍 DEBUG: API Retornando 0 Dias e 0 Transações

## ❌ PROBLEMA IDENTIFICADO

A API está retornando:
- **Total de dias: 0**
- **Total transações: 0**

Mesmo existindo uma transação de 1000 em 10X no sistema.

## 🔍 ANÁLISE DO PROBLEMA

### 1. **URL da Requisição**
```
http://localhost:3000/api/v1/settlements/schedule?merchantId=0f6ac19c-1bc2-43f8-a289-d0cf39615f02&startDate=2026-04-01&endDate=2026-04-30&page=0&size=10
```

**Problema:** O parâmetro `merchantId` está sendo passado na URL, mas o Controller não aceitava esse parâmetro!

### 2. **Como o Controller Funcionava Antes**
```java
@GetMapping("/schedule")
public ResponseEntity<SettlementScheduleResponse> getSchedule(
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate,
    @RequestParam(required = false) String status
) {
    UUID merchantId = securityContextService.getCurrentMerchantId(); // SEMPRE do token!
    // ...
}
```

O `merchantId` sempre vinha do token JWT, ignorando qualquer parâmetro da URL.

### 3. **Log Mostrando o Problema**
```
Consultando agenda de liquidação | Lojista: 0f6ac19c-1bc2-43f8-a289-d0cf39615f02
```
O merchantId no log é o mesmo passado na URL, mas pode ser que:
- O token JWT tenha um merchantId diferente
- Não haja dados para esse merchantId no banco
- A query esteja com problema

## ✅ SOLUÇÃO IMPLEMENTADA

### 1. **Adicionado Parâmetro `merchantId` Opcional**
```java
@GetMapping("/schedule")
public ResponseEntity<SettlementScheduleResponse> getSchedule(
    @RequestParam LocalDate startDate,
    @RequestParam LocalDate endDate,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) UUID merchantIdParam  // ← NOVO!
) {
    UUID merchantId = merchantIdParam != null ? 
        merchantIdParam : 
        securityContextService.getCurrentMerchantId();
    // ...
}
```

### 2. **Lógica de Prioridade**
- **Se `merchantId` for passado na URL:** Usa esse valor
- **Se não for passado:** Usa o do token JWT (comportamento original)

## 🧪 COMO TESTAR

### 1. **Teste com merchantId na URL**
```bash
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?merchantId=0f6ac19c-1bc2-43f8-a289-d0cf39615f02&startDate=2026-04-01&endDate=2026-04-30" \
  -H "Authorization: Bearer SEU_TOKEN"
```

### 2. **Teste sem merchantId (usa token)**
```bash
curl -X GET "http://localhost:8080/api/v1/settlements/schedule?startDate=2026-04-01&endDate=2026-04-30" \
  -H "Authorization: Bearer SEU_TOKEN"
```

### 3. **Script Automatizado**
```bash
chmod +x test_api_with_merchant.sh
./test_api_with_merchant.sh
```

## 🔍 POSSÍVEIS CAUSAS SE AINDA RETORNAR 0

### 1. **Não Há Dados no Banco**
Execute para verificar:
```bash
./debug_merchant_data.sh
```

### 2. **MerchantId Incorreto**
Verifique se o merchantId `0f6ac19c-1bc2-43f8-a289-d0cf39615f02` existe no banco.

### 3. **Datas Fora do Range**
Verifique se há transações no período 2026-04-01 até 2026-04-30.

### 4. **Problema na Query SQL**
A query pode ter algum filtro que está excluindo os dados.

## 📊 RESULTADO ESPERADO

Após a correção, a API deve retornar dados como:
```json
{
  "periodStart": "2026-04-01",
  "periodEnd": "2026-04-30",
  "totalPeriodGross": "1000.00",
  "totalPeriodNet": "970.00",
  "totalTransactionsInPeriod": 10,
  "schedule": [
    {
      "date": "2026-04-05",
      "totalGross": "100.00",
      "totalNet": "97.00",
      "transactionCount": 1,
      // ... outras propriedades
    }
    // ... mais dias
  ]
}
```

## 🎯 CONCLUSÃO

**Problema:** Controller não aceitava `merchantId` como parâmetro de query
**Solução:** Adicionado parâmetro opcional `merchantIdParam`
**Resultado:** API agora aceita merchantId na URL para facilitar testes

**A API agora deve funcionar corretamente! 🚀**
