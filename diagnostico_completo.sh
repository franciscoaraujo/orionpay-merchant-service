#!/bin/bash
# Script completo para diagnosticar o problema da API vazia

echo "🔍 DIAGNÓSTICO COMPLETO - API Settlement Schedule"
echo "=================================================="
echo ""

MERCHANT_ID="0f6ac19c-1bc2-43f8-a289-d0cf39615f02"
START_DATE="2026-04-01"
END_DATE="2026-04-30"

echo "📊 1. VERIFICANDO SE HÁ DADOS NO BANCO"
echo "========================================"
echo ""
echo "Query: SELECT COUNT(*) FROM ops.settlement_entry;"
echo "Resultado esperado: Número > 0"
echo ""

echo "📊 2. VERIFICANDO MERCHANTS EXISTENTES"
echo "======================================="
echo ""
echo "Query: SELECT merchant_id, COUNT(*) FROM ops.settlement_entry GROUP BY merchant_id ORDER BY COUNT(*) DESC LIMIT 5;"
echo "Resultado esperado: Pelo menos 1 merchant com registros"
echo ""

echo "📊 3. VERIFICANDO DATAS DISPONÍVEIS"
echo "===================================="
echo ""
echo "Query: SELECT MIN(expected_settlement_date), MAX(expected_settlement_date) FROM ops.settlement_entry;"
echo "Resultado esperado: Datas que incluem o período 2026-04-01 até 2026-04-30"
echo ""

echo "📊 4. VERIFICANDO DADOS PARA O MERCHANT ESPECÍFICO"
echo "==================================================="
echo ""
echo "Merchant ID: $MERCHANT_ID"
echo "Período: $START_DATE até $END_DATE"
echo ""
echo "Query:"
echo "SELECT COUNT(*) FROM ops.settlement_entry"
echo "WHERE merchant_id = '$MERCHANT_ID'"
echo "AND expected_settlement_date >= '$START_DATE'::date"
echo "AND expected_settlement_date <= '$END_DATE'::date;"
echo ""
echo "Resultado esperado: Número > 0"
echo ""

echo "📊 5. VERIFICANDO DISTRIBUIÇÃO POR DATA"
echo "========================================"
echo ""
echo "Query:"
echo "SELECT CAST(expected_settlement_date AS DATE) as date, COUNT(*) as transactions"
echo "FROM ops.settlement_entry"
echo "WHERE merchant_id = '$MERCHANT_ID'"
echo "AND expected_settlement_date >= '$START_DATE'::date"
echo "AND expected_settlement_date <= '$END_DATE'::date"
echo "GROUP BY CAST(expected_settlement_date AS DATE)"
echo "ORDER BY date;"
echo ""
echo "Resultado esperado: Lista de datas com contagens > 0"
echo ""

echo "🚀 6. TESTANDO API DIRETAMENTE"
echo "=============================="
echo ""
echo "URL: http://localhost:8080/api/v1/settlements/schedule"
echo "Parâmetros: merchantId=$MERCHANT_ID&startDate=$START_DATE&endDate=$END_DATE"
echo ""

# Teste da API
RESPONSE=$(curl -s -X GET "http://localhost:8080/api/v1/settlements/schedule?merchantId=$MERCHANT_ID&startDate=$START_DATE&endDate=$END_DATE" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c" \
  -H "Content-Type: application/json")

echo "Resposta da API:"
echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
echo ""

# Análise da resposta
TOTAL_TRANSACTIONS=$(echo "$RESPONSE" | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2 2>/dev/null || echo "0")
TOTAL_DAYS=$(echo "$RESPONSE" | grep -c '"date"' 2>/dev/null || echo "0")

echo "📈 ANÁLISE DA RESPOSTA:"
echo "======================="
echo "Total de transações retornadas: $TOTAL_TRANSACTIONS"
echo "Total de dias retornados: $TOTAL_DAYS"
echo ""

if [ "$TOTAL_TRANSACTIONS" = "0" ] && [ "$TOTAL_DAYS" = "0" ]; then
    echo "❌ DIAGNÓSTICO: API retornando vazia"
    echo ""
    echo "🔍 POSSÍVEIS CAUSAS:"
    echo "1. ❌ Não há dados no banco para o merchant $MERCHANT_ID"
    echo "2. ❌ As datas estão fora do range dos dados existentes"
    echo "3. ❌ Problema na query SQL (filtro incorreto)"
    echo "4. ❌ Dados não foram inseridos corretamente"
    echo ""
    echo "💡 PRÓXIMOS PASSOS:"
    echo "1. Execute as queries SQL acima no banco de dados"
    echo "2. Verifique se o merchant_id existe"
    echo "3. Verifique se há dados no período solicitado"
    echo "4. Verifique os logs da aplicação"
    echo ""
    echo "🔧 SOLUÇÕES POSSÍVEIS:"
    echo "1. Usar um merchant_id que existe no banco"
    echo "2. Ajustar as datas para um período que tem dados"
    echo "3. Verificar se a query SQL está correta"
    echo "4. Inserir dados de teste no banco"
else
    echo "✅ DIAGNÓSTICO: API retornando dados!"
    echo ""
    echo "🎯 A API está funcionando corretamente!"
fi

echo ""
echo "=================================================="
echo "FIM DO DIAGNÓSTICO"
echo "=================================================="
