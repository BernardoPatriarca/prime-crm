export type FieldType = 'TEXT' | 'NUMBER' | 'DATE' | 'SELECT' | 'MULTISELECT' | 'BOOLEAN';

export const FIELD_TYPES: FieldType[] = ['TEXT', 'NUMBER', 'DATE', 'SELECT', 'MULTISELECT', 'BOOLEAN'];

export interface CustomField {
  id: string;
  targetEntity: string;
  fieldKey: string;
  label: string;
  fieldType: FieldType;
  options: Record<string, unknown> | null;
  required: boolean;
  displayOrder: number;
  active: boolean;
}

export interface CustomFieldRequest {
  targetEntity: string;
  fieldKey: string;
  label: string;
  fieldType: FieldType;
  options?: Record<string, unknown> | null;
  required?: boolean;
  displayOrder?: number | null;
  active?: boolean;
}
