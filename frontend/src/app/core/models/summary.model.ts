export interface DomainValueSummary {
  id: string;
  code: string | null;
  name: string;
  color: string | null;
}

export interface UserSummary {
  id: string;
  name: string;
  email: string;
}

export interface CustomerSummary {
  id: string;
  code: string;
  name: string;
}

export interface ContactSummary {
  id: string;
  name: string;
  email: string | null;
}

export interface LeadSummary {
  id: string;
  code: string;
  name: string;
}

export interface OpportunitySummary {
  id: string;
  code: string;
  title: string;
}

export interface PipelineSummary {
  id: string;
  name: string;
}

export interface PipelineStageSummary {
  id: string;
  name: string;
  displayOrder: number;
  defaultProbability: number | null;
  color: string | null;
  requiresLossReason: boolean;
}
