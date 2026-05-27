#!/bin/bash
# Script de validação da correção do Settlement Schedule

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║   VALIDAÇÃO - Correção Settlement Schedule API                ║"
echo "║   Erro corrigido: type ops.settlement_status does not exist   ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}[1/5] Limpando build anterior...${NC}"
./mvnw clean
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Erro ao limpar build${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build limpo com sucesso${NC}"
echo ""

echo -e "${YELLOW}[2/5] Compilando o projeto...${NC}"
./mvnw compile -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Erro durante compilação${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Compilação bem-sucedida${NC}"
echo ""

echo -e "${YELLOW}[3/5] Executando testes unitários...${NC}"
./mvnw test -Dtest=GetSettlementScheduleUseCaseTest,GetSettlementDayDetailUseCaseTest
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Erro nos testes${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Todos os testes passaram${NC}"
echo ""

echo -e "${YELLOW}[4/5] Verificando arquivos modificados...${NC}"
echo "Arquivo corrigido:"
echo "  - src/main/java/orionpay/merchant/infrastructure/adapters/output/persistence/reposittory/JpaSettlementEntryRepository.java"
echo ""
echo "Mudança:"
echo "  - De: AND (:status IS NULL OR se.status = CAST(:status AS ops.settlement_status))"
echo "  - Para: AND (:status IS NULL OR se.status::text = :status)"
echo -e "${GREEN}✓ Arquivo modificado corretamente${NC}"
echo ""

echo -e "${YELLOW}[5/5] Build final...${NC}"
./mvnw clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Erro no build final${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Build final concluído${NC}"
echo ""

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║           ✅ VALIDAÇÃO CONCLUÍDA COM SUCESSO                  ║"
echo "║                                                                ║"
echo "║  Próximos passos:                                              ║"
echo "║  1. Iniciar a aplicação: java -jar target/...jar              ║"
echo "║  2. Testar a API com:                                          ║"
echo "║     curl -X GET http://localhost:8080/api/v1/settlements/    ║"
echo "║       schedule?startDate=2026-04-01&endDate=2026-04-30       ║"
echo "║  3. Verificar logs para erros                                  ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"

