import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AssignPermissionsRequest, Role, RoleRequest } from '../models/role.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface RoleListQuery {
  search?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/roles`;

  list(query: RoleListQuery): Observable<PageResponse<Role>> {
    return this.http.get<PageResponse<Role>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Role> {
    return this.http.get<Role>(`${this.baseUrl}/${id}`);
  }

  create(request: RoleRequest): Observable<Role> {
    return this.http.post<Role>(this.baseUrl, request);
  }

  update(id: string, request: RoleRequest): Observable<Role> {
    return this.http.put<Role>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  assignPermissions(id: string, request: AssignPermissionsRequest): Observable<Role> {
    return this.http.put<Role>(`${this.baseUrl}/${id}/permissions`, request);
  }
}
