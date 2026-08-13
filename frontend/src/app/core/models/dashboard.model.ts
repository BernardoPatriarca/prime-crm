export interface DashboardMetrics {
  newLeads: number;
  newLeadsTrend: number | null;
  convertedLeads: number;
  leadConversionRate: number;
  openOpportunities: number;
  openAmount: number;
  wonOpportunities: number;
  wonAmount: number;
  wonAmountTrend: number | null;
  lostOpportunities: number;
  winRate: number;
  averageTicket: number;
  activeCustomers: number;
  newCustomers: number;
  newCustomersTrend: number | null;
}

export interface DashboardFunnelStage {
  name: string;
  color: string | null;
  displayOrder: number;
  count: number;
  amount: number;
}

export interface DashboardFunnel {
  pipelineId: string | null;
  pipelineName: string | null;
  stages: DashboardFunnelStage[];
}

export interface DashboardMonthlyPoint {
  month: string;
  openedCount: number;
  openedAmount: number;
  wonCount: number;
  wonAmount: number;
}

export interface DashboardRankingRow {
  owner: string;
  count: number;
  amount: number;
  share: number;
}

export interface DashboardTaskSummary {
  pending: number;
  inProgress: number;
  overdue: number;
  dueToday: number;
  completedThisWeek: number;
}

export interface Dashboard {
  from: string;
  to: string;
  generatedAt: string;
  metrics: DashboardMetrics;
  funnel: DashboardFunnel;
  monthly: DashboardMonthlyPoint[];
  ranking: DashboardRankingRow[];
  tasks: DashboardTaskSummary;
}

export interface DashboardQuery {
  from?: string;
  to?: string;
  pipelineId?: string;
}
