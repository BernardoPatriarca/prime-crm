import { Permission } from './permission.model';

export interface Role {
  id: string;
  name: string;
  description: string | null;
  active: boolean;
  permissions: Permission[];
}

export interface RoleRequest {
  name: string;
  description?: string | null;
  active: boolean;
}

export interface AssignPermissionsRequest {
  permissionIds: string[];
}
