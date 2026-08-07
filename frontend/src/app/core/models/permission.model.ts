export type PermissionAction = 'VIEW' | 'CREATE' | 'EDIT' | 'DELETE' | 'EXPORT' | 'IMPORT';

export interface Permission {
  id: string;
  code: string;
  module: string;
  action: PermissionAction;
  description: string | null;
}
