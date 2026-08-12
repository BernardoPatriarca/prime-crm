import {
  ContactSummary,
  CustomerSummary,
  DomainValueSummary,
  LeadSummary,
  PipelineStageSummary,
  PipelineSummary,
  UserSummary
} from './summary.model';

export type OpportunityOutcome = 'OPEN' | 'WON' | 'LOST';

export interface Opportunity {
  id: string;
  code: string;
  title: string;
  customer: CustomerSummary | null;
  contact: ContactSummary | null;
  pipeline: PipelineSummary | null;
  stage: PipelineStageSummary | null;
  amount: number | null;
  probability: number | null;
  owner: UserSummary | null;
  team: DomainValueSummary | null;
  openedAt: string | null;
  expectedCloseDate: string | null;
  closedAt: string | null;
  outcome: OpportunityOutcome;
  winReason: DomainValueSummary | null;
  lossReason: DomainValueSummary | null;
  competitor: string | null;
  sourceLead: LeadSummary | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OpportunityRequest {
  title: string;
  customerId: string;
  contactId?: string | null;
  pipelineId: string;
  stageId?: string | null;
  amount?: number | null;
  probability?: number | null;
  ownerUserId?: string | null;
  teamId?: string | null;
  expectedCloseDate?: string | null;
  competitor?: string | null;
  sourceLeadId?: string | null;
  notes?: string | null;
}

export interface OpportunityStageMoveRequest {
  stageId: string;
  lossReasonId?: string | null;
  winReasonId?: string | null;
  note?: string | null;
}

export interface OpportunityCard {
  id: string;
  code: string;
  title: string;
  amount: number | null;
  probability: number | null;
  expectedCloseDate: string | null;
  openedAt: string | null;
  customer: CustomerSummary | null;
  owner: UserSummary | null;
}

export interface OpportunityBoardColumn {
  stageId: string;
  stageName: string;
  displayOrder: number;
  defaultProbability: number | null;
  color: string | null;
  requiresLossReason: boolean;
  totalCount: number;
  totalAmount: number | null;
  hasMore: boolean;
  opportunities: OpportunityCard[];
}

export interface OpportunityBoard {
  pipelineId: string;
  pipelineName: string;
  limitPerStage: number;
  totalCount: number;
  totalAmount: number | null;
  columns: OpportunityBoardColumn[];
}

export interface OpportunityStageHistory {
  id: string;
  fromStage: PipelineStageSummary | null;
  toStage: PipelineStageSummary | null;
  movedByUser: UserSummary | null;
  movedAt: string;
  daysInPreviousStage: number | null;
  note: string | null;
}
