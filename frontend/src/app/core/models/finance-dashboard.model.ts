export interface FinanceDashboardMetrics {
  openCount: number;
  openAmount: number;
  overdueCount: number;
  overdueAmount: number;
  movementCount: number;
  movementAmount: number;
  movementTrend: number | null;
}

export interface FinanceMonthlyPoint {
  month: string;
  receivedAmount: number;
  paidAmount: number;
  netAmount: number;
}

export interface FinanceDashboard {
  from: string;
  to: string;
  generatedAt: string;
  receivables: FinanceDashboardMetrics;
  payables: FinanceDashboardMetrics;
  monthly: FinanceMonthlyPoint[];
}

export interface FinanceDashboardQuery {
  from?: string;
  to?: string;
}
