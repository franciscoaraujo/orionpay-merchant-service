#!/bin/bash
# Script para testar se os filtros de status estão funcionando

echo "🧪 TESTANDO FILTROS DE STATUS - Settlement Schedule API"
echo "======================================================"
echo ""

BASE_URL="http://localhost:8080/api/v1/settlements/schedule"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
HEADERS="-H \"Authorization: Bearer $TOKEN\" -H \"Content-Type: application/json\""

echo "📊 Teste 1: SEM FILTRO (deve retornar todas as transações)"
echo "URL: $BASE_URL?startDate=2026-04-01&endDate=2026-04-30"
echo "Resultado esperado: 83 transações totais"
echo ""

# Teste sem filtro
RESPONSE1=$(curl -s -X GET "$BASE_URL?startDate=2026-04-01&endDate=2026-04-30" $HEADERS)
TOTAL1=$(echo $RESPONSE1 | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2)

echo "✅ Total sem filtro: $TOTAL1 transações"
echo ""

echo "📊 Teste 2: FILTRO AGENDADO (SCHEDULED)"
echo "URL: $BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=AGENDADO"
echo "Resultado esperado: Menos que 83 transações (apenas agendadas)"
echo ""

# Teste com filtro SCHEDULED
RESPONSE2=$(curl -s -X GET "$BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=AGENDADO" $HEADERS)
TOTAL2=$(echo $RESPONSE2 | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2)

echo "✅ Total com filtro AGENDADO: $TOTAL2 transações"
echo ""

echo "📊 Teste 3: FILTRO PAGO (PAID)"
echo "URL: $BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=PAGO"
echo "Resultado esperado: Menos que 83 transações (apenas pagas)"
echo ""

# Teste com filtro PAID
RESPONSE3=$(curl -s -X GET "$BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=PAGO" $HEADERS)
TOTAL3=$(echo $RESPONSE3 | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2)

echo "✅ Total com filtro PAGO: $TOTAL3 transações"
echo ""

echo "📊 Teste 4: FILTRO BLOQUEADO (BLOCKED)"
echo "URL: $BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=BLOQUEADO"
echo "Resultado esperado: Menos que 83 transações (apenas bloqueadas)"
echo ""

# Teste com filtro BLOCKED
RESPONSE4=$(curl -s -X GET "$BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=BLOQUEADO" $HEADERS)
TOTAL4=$(echo $RESPONSE4 | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2)

echo "✅ Total com filtro BLOQUEADO: $TOTAL4 transações"
echo ""

echo "📊 Teste 5: FILTRO ANTECIPADO (ANTICIPATED)"
echo "URL: $BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=ANTECIPADO"
echo "Resultado esperado: Menos que 83 transações (apenas antecipadas)"
echo ""

# Teste com filtro ANTICIPATED
RESPONSE5=$(curl -s -X GET "$BASE_URL?startDate=2026-04-01&endDate=2026-04-30&status=ANTECIPADO" $HEADERS)
TOTAL5=$(echo $RESPONSE5 | grep -o '"totalTransactionsInPeriod":[0-9]*' | cut -d':' -f2)

echo "✅ Total com filtro ANTECIPADO: $TOTAL5 transações"
echo ""

echo "📈 RESUMO DOS RESULTADOS"
echo "========================"
echo ""
echo "Sem filtro:     $TOTAL1 transações (100%)"
echo "Agendado:       $TOTAL2 transações"
echo "Pago:           $TOTAL3 transações"
echo "Bloqueado:      $TOTAL4 transações"
echo "Antecipado:     $TOTAL5 transações"
echo ""

# Verificar se os filtros estão funcionando
if [ "$TOTAL1" = "$TOTAL2" ] && [ "$TOTAL2" = "$TOTAL3" ] && [ "$TOTAL3" = "$TOTAL4" ] && [ "$TOTAL4" = "$TOTAL5" ]; then
    echo "❌ FALHA: Todos os filtros retornaram a mesma quantidade!"
    echo "   Os filtros NÃO estão funcionando."
    exit 1
else
    echo "✅ SUCESSO: Os filtros estão retornando quantidades diferentes!"
    echo "   Os filtros ESTÃO funcionando corretamente."
fi

echo ""
echo "🎯 CONCLUSÃO:"
echo "Se os números são diferentes, os filtros estão funcionando!"
echo "Se todos os números são iguais, há um problema na implementação."
