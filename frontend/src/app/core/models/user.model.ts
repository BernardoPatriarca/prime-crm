export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED';

export interface MeResponse {
  id: string;
  name: string;
  email: string;
  login: string;
  status: UserStatus;
  lastLoginAt: string | null;
  roles: string[];
  permissions: string[];
}

export type User = MeResponse;
