export interface DomainType {
  id: string;
  code: string;
  label: string;
  supportsColor: boolean;
  supportsIcon: boolean;
  systemDefined: boolean;
}

export interface DomainValue {
  id: string;
  domainTypeCode: string;
  domainTypeLabel: string;
  code: string | null;
  name: string;
  description: string | null;
  color: string | null;
  icon: string | null;
  displayOrder: number;
  extra: Record<string, unknown> | null;
  active: boolean;
}

export interface DomainValueRequest {
  domainTypeCode: string;
  code?: string | null;
  name: string;
  description?: string | null;
  color?: string | null;
  icon?: string | null;
  displayOrder?: number | null;
  extra?: Record<string, unknown> | null;
  active?: boolean;
}
