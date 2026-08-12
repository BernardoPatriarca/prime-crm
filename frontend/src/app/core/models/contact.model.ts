import { CustomerSummary, DomainValueSummary } from './summary.model';

export interface Contact {
  id: string;
  customer: CustomerSummary | null;
  name: string;
  positionTitle: string | null;
  department: DomainValueSummary | null;
  email: string | null;
  phone: string | null;
  mobile: string | null;
  birthDate: string | null;
  linkedin: string | null;
  primaryContact: boolean;
  decisionMaker: boolean;
  notes: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ContactRequest {
  customerId: string;
  name: string;
  positionTitle?: string | null;
  departmentId?: string | null;
  email?: string | null;
  phone?: string | null;
  mobile?: string | null;
  birthDate?: string | null;
  linkedin?: string | null;
  primaryContact?: boolean;
  decisionMaker?: boolean;
  notes?: string | null;
  active?: boolean;
}
