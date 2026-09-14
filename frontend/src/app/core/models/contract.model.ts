import { CustomerSummary, DomainValueSummary, OpportunitySummary, OrderSummary, UserSummary } from './summary.model';

export type ContractStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED';

export const CONTRACT_STATUSES: ContractStatus[] = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'TERMINATED'];

export interface Contract {
  id: string;
  code: string;
  customer: CustomerSummary | null;
  order: OrderSummary | null;
  opportunity: OpportunitySummary | null;
  owner: UserSummary | null;
  billingCycle: DomainValueSummary | null;
  status: ContractStatus;
  startDate: string;
  endDate: string | null;
  autoRenew: boolean;
  recurringAmount: number;
  notes: string | null;
  expired: boolean;
  terminatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ContractRequest {
  customerId: string;
  opportunityId?: string | null;
  ownerUserId?: string | null;
  billingCycleId?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  autoRenew: boolean;
  recurringAmount?: number | null;
  notes?: string | null;
}

export interface ContractStatusUpdateRequest {
  status: ContractStatus;
}
