import { Customer } from './customer.model';
import { CustomerSummary, DomainValueSummary, PipelineStageSummary, PipelineSummary, UserSummary } from './summary.model';

export interface Lead {
  id: string;
  code: string;
  name: string;
  companyName: string | null;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  mobile: string | null;
  origin: DomainValueSummary | null;
  status: DomainValueSummary | null;
  priority: DomainValueSummary | null;
  campaign: string | null;
  owner: UserSummary | null;
  pipeline: PipelineSummary | null;
  stage: PipelineStageSummary | null;
  probability: number | null;
  estimatedValue: number | null;
  expectedCloseDate: string | null;
  qualificationScore: number | null;
  convertedCustomer: CustomerSummary | null;
  convertedAt: string | null;
  notes: string | null;
  active: boolean;
  tags: DomainValueSummary[];
  createdAt: string;
  updatedAt: string;
}

export interface LeadRequest {
  name: string;
  companyName?: string | null;
  contactName?: string | null;
  email?: string | null;
  phone?: string | null;
  mobile?: string | null;
  originId?: string | null;
  statusId?: string | null;
  priorityId?: string | null;
  campaign?: string | null;
  ownerUserId?: string | null;
  pipelineId?: string | null;
  stageId?: string | null;
  probability?: number | null;
  estimatedValue?: number | null;
  expectedCloseDate?: string | null;
  qualificationScore?: number | null;
  notes?: string | null;
  tagIds?: string[];
  active?: boolean;
}

export interface LeadConvertRequest {
  clientTypeId?: string | null;
  segmentId?: string | null;
  ownerUserId?: string | null;
  createOpportunity?: boolean;
  pipelineId?: string | null;
  stageId?: string | null;
  opportunityTitle?: string | null;
  amount?: number | null;
  expectedCloseDate?: string | null;
}

export interface ConvertedOpportunity {
  id: string;
  code: string;
  title: string;
}

export interface LeadConvertResponse {
  lead: Lead;
  customer: Customer;
  opportunity: ConvertedOpportunity | null;
}
