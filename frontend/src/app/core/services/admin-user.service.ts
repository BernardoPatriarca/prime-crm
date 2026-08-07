import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AdminUser,
  AssignRolesRequest,
  ChangePasswordRequest,
  UserCreateRequest,
  UserStatusUpdateRequest,
  UserUpdateRequest
} from '../models/admin-user.model';
import { PageResponse } from '../models/page.model';
import { UserStatus } from '../models/user.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface AdminUserListQuery {
  search?: string;
  status?: UserStatus;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/users`;

  list(query: AdminUserListQuery): Observable<PageResponse<AdminUser>> {
    return this.http.get<PageResponse<AdminUser>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<AdminUser> {
    return this.http.get<AdminUser>(`${this.baseUrl}/${id}`);
  }

  create(request: UserCreateRequest): Observable<AdminUser> {
    return this.http.post<AdminUser>(this.baseUrl, request);
  }

  update(id: string, request: UserUpdateRequest): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  updateStatus(id: string, request: UserStatusUpdateRequest): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.baseUrl}/${id}/status`, request);
  }

  assignRoles(id: string, request: AssignRolesRequest): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}/${id}/roles`, request);
  }

  changePassword(id: string, request: ChangePasswordRequest): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/${id}/password`, request);
  }
}
