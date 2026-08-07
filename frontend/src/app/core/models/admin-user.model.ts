import { UserStatus } from './user.model';

export interface RoleSummary {
  id: string;
  name: string;
}

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  login: string;
  phone: string | null;
  status: UserStatus;
  lastLoginAt: string | null;
  createdAt: string;
  roles: RoleSummary[];
}

export interface UserCreateRequest {
  name: string;
  email: string;
  login: string;
  password: string;
  phone?: string | null;
}

export interface UserUpdateRequest {
  name: string;
  email: string;
  login: string;
  phone?: string | null;
}

export interface UserStatusUpdateRequest {
  status: UserStatus;
}

export interface AssignRolesRequest {
  roleIds: string[];
}

export interface ChangePasswordRequest {
  newPassword: string;
}
