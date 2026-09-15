import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { SalesGoal, SalesGoalRequest } from '../models/sales-goal.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface SalesGoalListQuery {
  search?: string;
  ownerUserId?: string;
  referenceMonth?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class SalesGoalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/sales-goals`;

  list(query: SalesGoalListQuery): Observable<PageResponse<SalesGoal>> {
    return this.http.get<PageResponse<SalesGoal>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<SalesGoal> {
    return this.http.get<SalesGoal>(`${this.baseUrl}/${id}`);
  }

  create(request: SalesGoalRequest): Observable<SalesGoal> {
    return this.http.post<SalesGoal>(this.baseUrl, request);
  }

  update(id: string, request: SalesGoalRequest): Observable<SalesGoal> {
    return this.http.put<SalesGoal>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
