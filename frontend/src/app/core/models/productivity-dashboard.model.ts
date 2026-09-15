import { DashboardTaskSummary } from './dashboard.model';

export interface ProductivityRankingRow {
  owner: string;
  completedCount: number;
  share: number;
}

export interface ProductivityAgendaSummary {
  scheduledToday: number;
  scheduledThisWeek: number;
  overdue: number;
}

export interface ProductivityDashboard {
  from: string;
  to: string;
  generatedAt: string;
  tasks: DashboardTaskSummary;
  taskRanking: ProductivityRankingRow[];
  agenda: ProductivityAgendaSummary;
}

export interface ProductivityDashboardQuery {
  from?: string;
  to?: string;
}
