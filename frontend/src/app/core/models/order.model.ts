import { CustomerSummary, OpportunitySummary, ProductSummary, ProposalSummary, UserSummary } from './summary.model';

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'DELIVERED' | 'CANCELED';

export const ORDER_STATUSES: OrderStatus[] = ['PENDING', 'CONFIRMED', 'DELIVERED', 'CANCELED'];

export interface Order {
  id: string;
  code: string;
  customer: CustomerSummary | null;
  proposal: ProposalSummary | null;
  opportunity: OpportunitySummary | null;
  owner: UserSummary | null;
  status: OrderStatus;
  orderDate: string;
  deliveryDate: string | null;
  notes: string | null;
  totalAmount: number;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OrderRequest {
  customerId: string;
  opportunityId?: string | null;
  ownerUserId?: string | null;
  orderDate?: string | null;
  deliveryDate?: string | null;
  notes?: string | null;
}

export interface OrderStatusUpdateRequest {
  status: OrderStatus;
}

export interface OrderItem {
  id: string;
  product: ProductSummary | null;
  description: string | null;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  total: number;
  displayOrder: number;
}

export interface OrderItemRequest {
  productId: string;
  description?: string | null;
  quantity: number;
  unitPrice?: number | null;
  discountPercent?: number | null;
  displayOrder?: number | null;
}
