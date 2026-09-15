import { UserSummary } from './summary.model';

export interface SalesGoal {
  id: string;
  owner: UserSummary | null;
  referenceMonth: string;
  targetAmount: number;
  realizedAmount: number;
  achievementPercent: number;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface SalesGoalRequest {
  ownerUserId: string;
  referenceMonth: string;
  targetAmount: number;
  notes?: string | null;
}
