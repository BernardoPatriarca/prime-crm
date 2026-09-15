import { ContractSummary, CustomerSummary, DomainValueSummary, OrderSummary } from './summary.model';

export type ReceivableStatus = 'PENDING' | 'PAID' | 'CANCELED';

export const RECEIVABLE_STATUSES: ReceivableStatus[] = ['PENDING', 'PAID', 'CANCELED'];

export interface Receivable {
  id: string;
  code: string;
  customer: CustomerSummary | null;
  order: OrderSummary | null;
  contract: ContractSummary | null;
  description: string | null;
  installmentNumber: number;
  totalInstallments: number;
  dueDate: string;
  amount: number;
  paidAmount: number;
  remainingAmount: number;
  paidAt: string | null;
  paymentMethod: DomainValueSummary | null;
  status: ReceivableStatus;
  overdue: boolean;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReceivableRequest {
  customerId: string;
  orderId?: string | null;
  contractId?: string | null;
  description?: string | null;
  installmentNumber: number;
  totalInstallments: number;
  dueDate: string;
  amount: number;
  paymentMethodId?: string | null;
  notes?: string | null;
}

export interface ReceivablePaymentRequest {
  amount?: number | null;
  paidAt?: string | null;
  paymentMethodId?: string | null;
}

export interface GenerateInstallmentsRequest {
  installments: number;
  firstDueDate: string;
}
