import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Opportunity,
  OpportunityBoard,
  OpportunityOutcome,
  OpportunityRequest,
  OpportunityStageHistory,
  OpportunityStageMoveRequest
} from '../models/opportunity.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface OpportunityListQuery {
  search?: string;
  pipelineId?: string;
  stageId?: string;
  customerId?: string;
  ownerUserId?: string;
  outcome?: OpportunityOutcome;
  expectedCloseFrom?: string;
  expectedCloseTo?: string;
  amountFrom?: number;
  amountTo?: number;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class OpportunityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/opportunities`;

  list(query: OpportunityListQuery): Observable<PageResponse<Opportunity>> {
    return this.http.get<PageResponse<Opportunity>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  board(pipelineId: string, limitPerStage?: number): Observable<OpportunityBoard> {
    return this.http.get<OpportunityBoard>(`${this.baseUrl}/board`, {
      params: buildHttpParams({ pipelineId, limitPerStage })
    });
  }

  getById(id: string): Observable<Opportunity> {
    return this.http.get<Opportunity>(`${this.baseUrl}/${id}`);
  }

  history(id: string): Observable<OpportunityStageHistory[]> {
    return this.http.get<OpportunityStageHistory[]>(`${this.baseUrl}/${id}/history`);
  }

  create(request: OpportunityRequest): Observable<Opportunity> {
    return this.http.post<Opportunity>(this.baseUrl, request);
  }

  update(id: string, request: OpportunityRequest): Observable<Opportunity> {
    return this.http.put<Opportunity>(`${this.baseUrl}/${id}`, request);
  }

  moveStage(id: string, request: OpportunityStageMoveRequest): Observable<Opportunity> {
    return this.http.patch<Opportunity>(`${this.baseUrl}/${id}/stage`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
