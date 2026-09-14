import { DomainValueSummary } from './summary.model';

export interface Product {
  id: string;
  code: string;
  name: string;
  description: string | null;
  sku: string | null;
  category: DomainValueSummary | null;
  unit: DomainValueSummary | null;
  unitPrice: number;
  costPrice: number | null;
  service: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  description?: string | null;
  sku?: string | null;
  categoryId?: string | null;
  unitId?: string | null;
  unitPrice: number;
  costPrice?: number | null;
  service: boolean;
  active: boolean;
}
