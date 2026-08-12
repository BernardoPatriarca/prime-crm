import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Lead, LeadConvertRequest, LeadConvertResponse, LeadRequest } from '../models/lead.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface LeadListQuery {
  search?: string;
  originId?: string;
  statusId?: string;
  priorityId?: string;
  ownerUserId?: string;
  pipelineId?: string;
  stageId?: string;
  active?: boolean;
  expectedCloseFrom?: string;
  expectedCloseTo?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class LeadService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/leads`;

  list(query: LeadListQuery): Observable<PageResponse<Lead>> {
    return this.http.get<PageResponse<Lead>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Lead> {
    return this.http.get<Lead>(`${this.baseUrl}/${id}`);
  }

  create(request: LeadRequest): Observable<Lead> {
    return this.http.post<Lead>(this.baseUrl, request);
  }

  update(id: string, request: LeadRequest): Observable<Lead> {
    return this.http.put<Lead>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  convert(id: string, request: LeadConvertRequest): Observable<LeadConvertResponse> {
    return this.http.post<LeadConvertResponse>(`${this.baseUrl}/${id}/convert`, request);
  }
}
