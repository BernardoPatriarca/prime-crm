import { ContactSummary, CustomerSummary, OpportunitySummary, ProductSummary, UserSummary } from './summary.model';

export type ProposalStatus = 'DRAFT' | 'SENT' | 'ACCEPTED' | 'REJECTED';

export const PROPOSAL_STATUSES: ProposalStatus[] = ['DRAFT', 'SENT', 'ACCEPTED', 'REJECTED'];

export interface Proposal {
  id: string;
  code: string;
  customer: CustomerSummary | null;
  contact: ContactSummary | null;
  opportunity: OpportunitySummary | null;
  owner: UserSummary | null;
  status: ProposalStatus;
  issueDate: string;
  validUntil: string | null;
  notes: string | null;
  totalAmount: number;
  expired: boolean;
  decidedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ProposalRequest {
  customerId: string;
  contactId?: string | null;
  opportunityId?: string | null;
  ownerUserId?: string | null;
  issueDate?: string | null;
  validUntil?: string | null;
  notes?: string | null;
}

export interface ProposalStatusUpdateRequest {
  status: ProposalStatus;
}

export interface ProposalItem {
  id: string;
  product: ProductSummary | null;
  description: string | null;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  total: number;
  displayOrder: number;
}

export interface ProposalItemRequest {
  productId: string;
  description?: string | null;
  quantity: number;
  unitPrice?: number | null;
  discountPercent?: number | null;
  displayOrder?: number | null;
}
