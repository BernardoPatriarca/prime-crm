export interface CommercialDashboardMetrics {
  totalCount: number;
  totalAmount: number;
  closedCount: number;
  closedAmount: number;
  conversionRate: number;
}

export interface ContractDashboardMetrics {
  activeCount: number;
  activeRecurringAmount: number;
  expiringCount: number;
  expiringAmount: number;
}

export interface CommercialMonthlyPoint {
  month: string;
  proposalsAmount: number;
  ordersAmount: number;
}

export interface CommercialDashboard {
  from: string;
  to: string;
  generatedAt: string;
  proposals: CommercialDashboardMetrics;
  orders: CommercialDashboardMetrics;
  contracts: ContractDashboardMetrics;
  monthly: CommercialMonthlyPoint[];
}

export interface CommercialDashboardQuery {
  from?: string;
  to?: string;
}
