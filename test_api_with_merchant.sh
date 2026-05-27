#!/bin/bash
# Teste da API com merchantId como parâmetro

echo "🧪 TESTANDO API COM MERCHANTID COMO PARÂMETRO"
echo "=============================================="
echo ""

MERCHANT_ID="0f6ac19c-1bc2-43f8-a289-d0cf39615f02"
START_DATE="2026-04-01"
END_DATE="2026-04-30"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

echo "📊 Teste 1: API com merchantId como parâmetro"
echo "URL: http://localhost:8080/api/v1/settlements/schedule?merchantId=$MERCHANT_ID&startDate=$START_DATE&endDate=$END_DATE"
echo ""

RESPONSE=$(curl -s -X GET "http://localhost:8080/api/v1/settlements/schedule?merchantId=$MERCHANT_ID&startDate=$START_DATE&endDate=$END_DATE" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Resposta da API:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

# Extrair valores da resposta
TOTAL_TRANSACTIONS=$(echo "$RESPONSE" | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2)
TOTAL_DAYS=$(echo "$RESPONSE" | grep -o '"schedule":\[[^]]*\]' | grep -o '"date"' | wc -l)

echo "📈 RESULTADOS:"
echo "=============="
echo "Total de transações: $TOTAL_TRANSACTIONS"
echo "Total de dias: $TOTAL_DAYS"
echo ""

if [ "$TOTAL_TRANSACTIONS" = "0" ] && [ "$TOTAL_DAYS" = "0" ]; then
    echo "❌ RESULTADO: Nenhum dado encontrado"
    echo ""
    echo "🔍 POSSÍVEIS CAUSAS:"
    echo "1. Não há dados para o merchantId $MERCHANT_ID"
    echo "2. As datas estão fora do range dos dados"
    echo "3. Problema na query SQL"
    echo "4. Dados não foram inseridos no banco"
    echo ""
    echo "💡 PRÓXIMOS PASSOS:"
    echo "1. Verificar se há dados no banco para este merchant"
    echo "2. Executar: ./debug_merchant_data.sh"
    echo "3. Verificar logs do banco de dados"
else
    echo "✅ RESULTADO: Dados encontrados!"
    echo ""
    echo "🎯 A API está funcionando com merchantId como parâmetro!"
fi
