/**
 * Integração Frontend para Settlement Schedule API
 *
 * Este arquivo contém exemplos de como integrar o endpoint de agenda de liquidação
 * com uma aplicação frontend (React, Angular, Vue, etc).
 */

// ============================================================================
// 1. SERVIÇO DE API (Service/Repository Pattern)
// ============================================================================

interface DailySchedule {
  date: string;
  totalGross: number;
  totalNet: number;
  mdrAmount: number;
  statusSummary: string[];
  transactionCount: number;
  dailyAverageTransaction: number;
}

interface SettlementScheduleResponse {
  periodStart: string;
  periodEnd: string;
  totalPeriodGross: number;
  totalPeriodNet: number;
  totalTransactionsInPeriod: number;
  schedule: DailySchedule[];
}

interface TransactionDetail {
  idExt: string;
  transactionId: string;
  nsu: string;
  transactionDate: string;
  settlementDate: string;
  paidAt: string | null;
  grossAmount: number;
  originalAmount: number;
  mdrPercentage: number;
  mdrAmount: number;
  netAmount: number;
  cardBrand: string;
  cardLastFour: string;
  productType: string;
  blocked: boolean;
  anticipated: boolean;
  installmentNumber: number;
  installmentLabel: string;
  status: string;
}

interface SettlementDayDetailResponse {
  settlementDate: string;
  totalGross: number;
  totalMdr: number;
  totalNet: number;
  averageTransaction: number;
  totalCount: number;
  blockedCount: number;
  anticipatedCount: number;
  statusBreakdown: Record<string, number>;
  transactions: TransactionDetail[];
  pageNumber: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

// ============================================================================
// 2. SERVIÇO DE API - TypeScript/JavaScript
// ============================================================================

class SettlementScheduleService {
  private baseUrl = '/api/v1/settlements';
  private authToken = localStorage.getItem('authToken');

  /**
   * Recupera a agenda de liquidação para um período
   */
  async getSchedule(
    startDate: Date,
    endDate: Date,
    status?: string
  ): Promise<SettlementScheduleResponse> {
    const params = new URLSearchParams({
      startDate: this.formatDate(startDate),
      endDate: this.formatDate(endDate),
    });

    if (status) {
      params.append('status', status);
    }

    const response = await fetch(`${this.baseUrl}/schedule?${params}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${this.authToken}`,
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`Erro ao buscar agenda: ${response.statusText}`);
    }

    return response.json();
  }

  /**
   * Recupera o detalhe de um dia específico
   */
  async getScheduleDayDetail(
    settlementDate: Date,
    status?: string,
    page: number = 0,
    pageSize: number = 50
  ): Promise<SettlementDayDetailResponse> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: pageSize.toString(),
    });

    if (status) {
      params.append('status', status);
    }

    const dateStr = this.formatDate(settlementDate);
    const response = await fetch(
      `${this.baseUrl}/schedule/${dateStr}?${params}`,
      {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.authToken}`,
          'Content-Type': 'application/json',
        },
      }
    );

    if (!response.ok) {
      throw new Error(`Erro ao buscar detalhe do dia: ${response.statusText}`);
    }

    return response.json();
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}

// ============================================================================
// 3. COMPONENTE REACT - Agenda Mensal
// ============================================================================

import React, { useState, useEffect } from 'react';

interface AgendaProps {
  startDate: Date;
  endDate: Date;
}

const AgendaComponent: React.FC<AgendaProps> = ({ startDate, endDate }) => {
  const [schedule, setSchedule] = useState<SettlementScheduleResponse | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const service = new SettlementScheduleService();

  useEffect(() => {
    loadSchedule();
  }, [startDate, endDate, selectedStatus]);

  const loadSchedule = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await service.getSchedule(startDate, endDate, selectedStatus);
      setSchedule(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro desconhecido');
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Carregando...</div>;
  if (error) return <div>Erro: {error}</div>;
  if (!schedule) return <div>Sem dados</div>;

  return (
    <div className="settlement-schedule">
      <h2>Agenda de Liquidação</h2>

      {/* Filtro por Status */}
      <div className="filters">
        <select
          value={selectedStatus || ''}
          onChange={(e) => setSelectedStatus(e.target.value || undefined)}
        >
          <option value="">Todos os Status</option>
          <option value="SCHEDULED">Agendado</option>
          <option value="SETTLED">Liquidado</option>
          <option value="PENDING">Pendente</option>
          <option value="ANTICIPATED">Antecipado</option>
          <option value="PAID">Pago</option>
        </select>
      </div>

      {/* Totalizadores do Período */}
      <div className="period-summary">
        <div className="card">
          <h3>Total Bruto</h3>
          <p>{formatCurrency(schedule.totalPeriodGross)}</p>
        </div>
        <div className="card">
          <h3>Total Líquido</h3>
          <p>{formatCurrency(schedule.totalPeriodNet)}</p>
        </div>
        <div className="card">
          <h3>Transações</h3>
          <p>{schedule.totalTransactionsInPeriod}</p>
        </div>
        <div className="card">
          <h3>MDR Total</h3>
          <p>
            {formatCurrency(
              schedule.totalPeriodGross - schedule.totalPeriodNet
            )}
          </p>
        </div>
      </div>

      {/* Tabela de Dias */}
      <table className="schedule-table">
        <thead>
          <tr>
            <th>Data</th>
            <th>Transações</th>
            <th>Total Bruto</th>
            <th>MDR</th>
            <th>Total Líquido</th>
            <th>Ticket Médio</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {schedule.schedule.map((day) => (
            <tr key={day.date} className="clickable">
              <td>{formatDate(new Date(day.date))}</td>
              <td>{day.transactionCount}</td>
              <td>{formatCurrency(day.totalGross)}</td>
              <td>{formatCurrency(day.mdrAmount)}</td>
              <td>{formatCurrency(day.totalNet)}</td>
              <td>{formatCurrency(day.dailyAverageTransaction)}</td>
              <td>
                <span className="status-badges">
                  {day.statusSummary.map((status) => (
                    <span key={status} className={`badge badge-${status}`}>
                      {translateStatus(status)}
                    </span>
                  ))}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

// ============================================================================
// 4. COMPONENTE REACT - Detalhe do Dia
// ============================================================================

interface DayDetailProps {
  settlementDate: Date;
}

const DayDetailComponent: React.FC<DayDetailProps> = ({ settlementDate }) => {
  const [detail, setDetail] = useState<SettlementDayDetailResponse | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<string | undefined>();
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  const service = new SettlementScheduleService();

  useEffect(() => {
    loadDetail();
  }, [settlementDate, selectedStatus, page]);

  const loadDetail = async () => {
    setLoading(true);
    try {
      const data = await service.getScheduleDayDetail(
        settlementDate,
        selectedStatus,
        page,
        50
      );
      setDetail(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div>Carregando...</div>;
  if (!detail) return <div>Sem dados</div>;

  return (
    <div className="day-detail">
      <h2>Detalhe de {formatDate(settlementDate)}</h2>

      {/* Resumo do Dia */}
      <div className="day-summary">
        <div className="card">
          <h3>Total Bruto</h3>
          <p>{formatCurrency(detail.totalGross)}</p>
        </div>
        <div className="card">
          <h3>Total Líquido</h3>
          <p>{formatCurrency(detail.totalNet)}</p>
        </div>
        <div className="card">
          <h3>Total MDR</h3>
          <p>{formatCurrency(detail.totalMdr)}</p>
        </div>
        <div className="card">
          <h3>Ticket Médio</h3>
          <p>{formatCurrency(detail.averageTransaction)}</p>
        </div>
        <div className="card">
          <h3>Transações</h3>
          <p>{detail.totalCount}</p>
        </div>
        <div className="card">
          <h3>Bloqueadas</h3>
          <p>{detail.blockedCount}</p>
        </div>
        <div className="card">
          <h3>Antecipadas</h3>
          <p>{detail.anticipatedCount}</p>
        </div>
      </div>

      {/* Breakdown de Status */}
      <div className="status-breakdown">
        <h3>Distribuição de Status</h3>
        {Object.entries(detail.statusBreakdown).map(([status, count]) => (
          <div key={status} className="status-item">
            <span>{translateStatus(status)}</span>
            <span>{count} transações</span>
          </div>
        ))}
      </div>

      {/* Tabela de Transações */}
      <table className="transactions-table">
        <thead>
          <tr>
            <th>NSU</th>
            <th>Cartão</th>
            <th>Data Transação</th>
            <th>Valor Bruto</th>
            <th>MDR</th>
            <th>Valor Líquido</th>
            <th>Parcela</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {detail.transactions.map((tx) => (
            <tr key={tx.idExt}>
              <td>{tx.nsu}</td>
              <td>{tx.cardBrand} •••• {tx.cardLastFour}</td>
              <td>{formatDateTime(new Date(tx.transactionDate))}</td>
              <td>{formatCurrency(tx.grossAmount)}</td>
              <td>{formatCurrency(tx.mdrAmount)}</td>
              <td>{formatCurrency(tx.netAmount)}</td>
              <td>{tx.installmentLabel}</td>
              <td>
                <span className={`badge badge-${tx.status}`}>
                  {translateStatus(tx.status)}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Paginação */}
      {detail.totalPages > 1 && (
        <div className="pagination">
          <button
            onClick={() => setPage(page - 1)}
            disabled={page === 0}
          >
            Anterior
          </button>
          <span>Página {page + 1} de {detail.totalPages}</span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={page >= detail.totalPages - 1}
          >
            Próxima
          </button>
        </div>
      )}
    </div>
  );
};

// ============================================================================
// 5. FUNÇÕES UTILITÁRIAS
// ============================================================================

function formatCurrency(value: number | null | undefined): string {
  if (!value && value !== 0) return 'R$ 0,00';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
}

function formatDate(date: Date): string {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
}

function formatDateTime(date: Date): string {
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function translateStatus(status: string): string {
  const statusMap: Record<string, string> = {
    PENDING: 'Pendente',
    SCHEDULED: 'Agendado',
    ANTICIPATED: 'Antecipado',
    BLOCKED: 'Bloqueado',
    SETTLED: 'Liquidado',
    PAID: 'Pago',
    DISPUTE: 'Disputa',
    FAILED: 'Falha',
    PREPAID: 'Pré-pago',
  };
  return statusMap[status] || status;
}

// ============================================================================
// 6. EXEMPLO DE USO EM PÁGINA COMPLETA
// ============================================================================

const SettlementPage: React.FC = () => {
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [showDetail, setShowDetail] = useState(false);

  const today = new Date();
  const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1);
  const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0);

  return (
    <div className="settlement-page">
      <h1>Liquidações</h1>

      {!showDetail ? (
        <AgendaComponent startDate={startOfMonth} endDate={endOfMonth} />
      ) : (
        <DayDetailComponent settlementDate={selectedDate || today} />
      )}

      {showDetail && (
        <button onClick={() => setShowDetail(false)}>
          Voltar para Agenda
        </button>
      )}
    </div>
  );
};

export { SettlementScheduleService, AgendaComponent, DayDetailComponent };

