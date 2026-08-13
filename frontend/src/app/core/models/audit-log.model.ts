export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'LOGIN' | 'LOGIN_FAILED' | 'LOGOUT' | 'EXPORT';

export const AUDIT_ACTIONS: AuditAction[] = [
  'CREATE',
  'UPDATE',
  'DELETE',
  'LOGIN',
  'LOGIN_FAILED',
  'LOGOUT',
  'EXPORT'
];

export interface AuditChange {
  old: unknown;
  new: unknown;
}

export interface AuditLog {
  id: string;
  entityName: string;
  entityId: string | null;
  action: AuditAction;
  changes: Record<string, unknown> | null;
  userId: string | null;
  userEmail: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
}
