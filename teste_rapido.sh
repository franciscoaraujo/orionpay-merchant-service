#!/bin/bash
# Teste rápido para diagnosticar o problema da API vazia

echo "🔍 TESTE RÁPIDO - Settlement Schedule API"
echo "========================================"
echo ""

MERCHANT_ID="0f6ac19c-1bc2-43f8-a289-d0cf39615f02"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

echo "1️⃣ Testando endpoint de DEBUG:"
echo "=============================="
echo "URL: http://localhost:8080/api/v1/settlements/schedule/debug?merchantId=$MERCHANT_ID"
echo ""

DEBUG_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/v1/settlements/schedule/debug?merchantId=$MERCHANT_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Resposta do endpoint debug:"
echo "$DEBUG_RESPONSE" | jq '.' 2>/dev/null || echo "$DEBUG_RESPONSE"
echo ""

TOTAL_RECORDS=$(echo "$DEBUG_RESPONSE" | grep -o '"totalRecordsInDb":[0-9]*' | cut -d':' -f2 2>/dev/null || echo "0")

echo "📊 RESULTADO DO DEBUG:"
echo "======================"
echo "Total de registros no banco: $TOTAL_RECORDS"
echo ""

if [ "$TOTAL_RECORDS" = "0" ]; then
    echo "❌ PROBLEMA IDENTIFICADO:"
    echo "========================="
    echo "Não há dados no banco para o merchantId $MERCHANT_ID"
    echo ""
    echo "🔍 POSSÍVEIS CAUSAS:"
    echo "1. O merchantId não existe no banco"
    echo "2. Não há dados de settlement_entry para este merchant"
    echo "3. Os dados não foram inseridos corretamente"
    echo ""
    echo "💡 SOLUÇÕES:"
    echo "1. Verificar se o merchant existe na tabela core.merchant"
    echo "2. Inserir dados de teste na tabela ops.settlement_entry"
    echo "3. Usar um merchantId diferente que tenha dados"
    echo ""
    echo "📝 SCRIPT PARA INSERIR DADOS DE TESTE:"
    echo "======================================"
    echo "-- Inserir dados de teste"
    echo "INSERT INTO ops.settlement_entry ("
    echo "  id, transaction_id, merchant_id, amount, net_amount,"
    echo "  expected_settlement_date, status, created_at, updated_at"
    echo ") VALUES ("
    echo "  gen_random_uuid(),"
    echo "  gen_random_uuid(),"
    echo "  '$MERCHANT_ID'::uuid,"
    echo "  100.00,"
    echo "  97.00,"
    echo "  '2026-04-05'::date,"
    echo "  'SCHEDULED',"
    echo "  now(),"
    echo "  now()"
    echo ");"
else
    echo "✅ DEBUG OK:"
    echo "============"
    echo "Há dados no banco! O problema pode ser:"
    echo "1. Filtro de datas (2026-04-01 até 2026-04-30)"
    echo "2. Filtro de status"
    echo "3. Problema na query SQL"
    echo ""
    echo "🔍 PRÓXIMOS TESTES:"
    echo "==================="
    echo "1. Testar com período maior (2000-01-01 até 2030-12-31)"
    echo "2. Testar sem filtros de status"
    echo "3. Verificar logs da aplicação"
fi

echo ""
echo "2️⃣ Testando API principal:"
echo "=========================="
echo "URL: http://localhost:8080/api/v1/settlements/schedule?merchantId=$MERCHANT_ID&startDate=2026-04-01&endDate=2026-04-30"
echo ""

API_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/v1/settlements/schedule?merchantId=$MERCHANT_ID&startDate=2026-04-01&endDate=2026-04-30" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Resposta da API principal:"
echo "$API_RESPONSE" | jq '.' 2>/dev/null || echo "$API_RESPONSE"
echo ""

TOTAL_TRANSACTIONS=$(echo "$API_RESPONSE" | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2 2>/dev/null || echo "0")
TOTAL_DAYS=$(echo "$API_RESPONSE" | grep -c '"date"' 2>/dev/null || echo "0")

echo "📈 RESULTADO FINAL:"
echo "==================="
echo "API Debug - Registros no banco: $TOTAL_RECORDS"
echo "API Principal - Transações retornadas: $TOTAL_TRANSACTIONS"
echo "API Principal - Dias retornados: $TOTAL_DAYS"
echo ""

if [ "$TOTAL_TRANSACTIONS" = "0" ] && [ "$TOTAL_RECORDS" != "0" ]; then
    echo "🔍 DIAGNÓSTICO:"
    echo "==============="
    echo "Há dados no banco ($TOTAL_RECORDS registros), mas a API retorna vazio."
    echo "Isso indica um problema na query SQL ou nos filtros aplicados."
    echo ""
    echo "Possíveis causas:"
    echo "- Filtro de datas muito restritivo"
    echo "- Filtro de status excluindo todos os registros"
    echo "- Problema na conversão de status (PT → EN)"
    echo "- Bug na query SQL"
elif [ "$TOTAL_TRANSACTIONS" = "0" ] && [ "$TOTAL_RECORDS" = "0" ]; then
    echo "🔍 DIAGNÓSTICO:"
    echo "==============="
    echo "Não há dados no banco para este merchantId."
    echo "É necessário inserir dados de teste primeiro."
else
    echo "✅ DIAGNÓSTICO:"
    echo "==============="
    echo "API funcionando corretamente!"
    echo "Encontrados $TOTAL_TRANSACTIONS transações em $TOTAL_DAYS dias."
fi

echo ""
echo "=================================================="
echo "FIM DO TESTE RÁPIDO"
echo "=================================================="
