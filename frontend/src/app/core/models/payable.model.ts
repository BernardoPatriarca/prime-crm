import { CustomerSummary, DomainValueSummary } from './summary.model';

export type PayableStatus = 'PENDING' | 'PAID' | 'CANCELED';

export const PAYABLE_STATUSES: PayableStatus[] = ['PENDING', 'PAID', 'CANCELED'];

export interface Payable {
  id: string;
  code: string;
  supplier: CustomerSummary | null;
  category: DomainValueSummary | null;
  description: string | null;
  dueDate: string;
  amount: number;
  paidAmount: number;
  remainingAmount: number;
  paidAt: string | null;
  paymentMethod: DomainValueSummary | null;
  status: PayableStatus;
  overdue: boolean;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PayableRequest {
  supplierId: string;
  categoryId?: string | null;
  description?: string | null;
  dueDate: string;
  amount: number;
  paymentMethodId?: string | null;
  notes?: string | null;
}

export interface PayablePaymentRequest {
  amount?: number | null;
  paidAt?: string | null;
  paymentMethodId?: string | null;
}
