import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Pipeline, PipelineRequest } from '../models/pipeline.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface PipelineListQuery {
  search?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class PipelineService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/pipelines`;

  list(query: PipelineListQuery): Observable<PageResponse<Pipeline>> {
    return this.http.get<PageResponse<Pipeline>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Pipeline> {
    return this.http.get<Pipeline>(`${this.baseUrl}/${id}`);
  }

  create(request: PipelineRequest): Observable<Pipeline> {
    return this.http.post<Pipeline>(this.baseUrl, request);
  }

  update(id: string, request: PipelineRequest): Observable<Pipeline> {
    return this.http.put<Pipeline>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
