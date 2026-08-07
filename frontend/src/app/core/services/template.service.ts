import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageTemplate, TemplateRequest, TemplateType } from '../models/template.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface TemplateListQuery {
  type?: TemplateType;
  search?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class TemplateService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/templates`;

  list(query: TemplateListQuery): Observable<PageResponse<MessageTemplate>> {
    return this.http.get<PageResponse<MessageTemplate>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<MessageTemplate> {
    return this.http.get<MessageTemplate>(`${this.baseUrl}/${id}`);
  }

  create(request: TemplateRequest): Observable<MessageTemplate> {
    return this.http.post<MessageTemplate>(this.baseUrl, request);
  }

  update(id: string, request: TemplateRequest): Observable<MessageTemplate> {
    return this.http.put<MessageTemplate>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
