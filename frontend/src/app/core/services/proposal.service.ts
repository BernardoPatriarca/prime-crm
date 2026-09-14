import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import {
  Proposal,
  ProposalItem,
  ProposalItemRequest,
  ProposalRequest,
  ProposalStatus,
  ProposalStatusUpdateRequest
} from '../models/proposal.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface ProposalListQuery {
  search?: string;
  status?: ProposalStatus;
  customerId?: string;
  opportunityId?: string;
  ownerUserId?: string;
  expired?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProposalService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/proposals`;

  list(query: ProposalListQuery): Observable<PageResponse<Proposal>> {
    return this.http.get<PageResponse<Proposal>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Proposal> {
    return this.http.get<Proposal>(`${this.baseUrl}/${id}`);
  }

  create(request: ProposalRequest): Observable<Proposal> {
    return this.http.post<Proposal>(this.baseUrl, request);
  }

  update(id: string, request: ProposalRequest): Observable<Proposal> {
    return this.http.put<Proposal>(`${this.baseUrl}/${id}`, request);
  }

  changeStatus(id: string, request: ProposalStatusUpdateRequest): Observable<Proposal> {
    return this.http.patch<Proposal>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listItems(proposalId: string): Observable<ProposalItem[]> {
    return this.http.get<ProposalItem[]>(`${this.baseUrl}/${proposalId}/items`);
  }

  createItem(proposalId: string, request: ProposalItemRequest): Observable<ProposalItem> {
    return this.http.post<ProposalItem>(`${this.baseUrl}/${proposalId}/items`, request);
  }

  updateItem(proposalId: string, itemId: string, request: ProposalItemRequest): Observable<ProposalItem> {
    return this.http.put<ProposalItem>(`${this.baseUrl}/${proposalId}/items/${itemId}`, request);
  }

  deleteItem(proposalId: string, itemId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${proposalId}/items/${itemId}`);
  }
}
