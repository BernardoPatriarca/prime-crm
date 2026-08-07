export type TemplateType = 'EMAIL' | 'PROPOSAL' | 'CONTRACT' | 'WHATSAPP';

export const TEMPLATE_TYPES: TemplateType[] = ['EMAIL', 'PROPOSAL', 'CONTRACT', 'WHATSAPP'];

export interface MessageTemplate {
  id: string;
  type: TemplateType;
  name: string;
  subject: string | null;
  content: string;
  active: boolean;
}

export interface TemplateRequest {
  type: TemplateType;
  name: string;
  subject?: string | null;
  content: string;
  active?: boolean;
}
