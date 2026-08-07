import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DomainValue, DomainValueRequest } from '../models/domain-value.model';
import { PageResponse } from '../models/page.model';
import { ReorderItem } from '../models/reorder.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface DomainValueListQuery {
  type?: string;
  search?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class DomainValueService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/domain-values`;

  list(query: DomainValueListQuery): Observable<PageResponse<DomainValue>> {
    return this.http.get<PageResponse<DomainValue>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<DomainValue> {
    return this.http.get<DomainValue>(`${this.baseUrl}/${id}`);
  }

  create(request: DomainValueRequest): Observable<DomainValue> {
    return this.http.post<DomainValue>(this.baseUrl, request);
  }

  update(id: string, request: DomainValueRequest): Observable<DomainValue> {
    return this.http.put<DomainValue>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  reorder(items: ReorderItem[]): Observable<DomainValue[]> {
    return this.http.put<DomainValue[]>(`${this.baseUrl}/reorder`, { items });
  }
}
