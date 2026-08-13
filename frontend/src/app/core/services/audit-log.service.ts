import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditAction, AuditLog } from '../models/audit-log.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface AuditLogListQuery {
  search?: string;
  entityName?: string;
  entityId?: string;
  action?: AuditAction;
  userId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/audit-logs`;

  list(query: AuditLogListQuery): Observable<PageResponse<AuditLog>> {
    return this.http.get<PageResponse<AuditLog>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  entityNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/entities`);
  }

  timeline(entityName: string, entityId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/${entityName}/${entityId}`);
  }

  export(query: AuditLogListQuery): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/export`, {
      params: buildHttpParams({ ...query }),
      responseType: 'blob'
    });
  }
}
