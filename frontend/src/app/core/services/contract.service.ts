import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contract, ContractRequest, ContractStatus, ContractStatusUpdateRequest } from '../models/contract.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface ContractListQuery {
  search?: string;
  status?: ContractStatus;
  customerId?: string;
  opportunityId?: string;
  ownerUserId?: string;
  expired?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ContractService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/contracts`;

  list(query: ContractListQuery): Observable<PageResponse<Contract>> {
    return this.http.get<PageResponse<Contract>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Contract> {
    return this.http.get<Contract>(`${this.baseUrl}/${id}`);
  }

  create(request: ContractRequest): Observable<Contract> {
    return this.http.post<Contract>(this.baseUrl, request);
  }

  createFromOrder(orderId: string): Observable<Contract> {
    return this.http.post<Contract>(`${this.baseUrl}/from-order/${orderId}`, {});
  }

  update(id: string, request: ContractRequest): Observable<Contract> {
    return this.http.put<Contract>(`${this.baseUrl}/${id}`, request);
  }

  changeStatus(id: string, request: ContractStatusUpdateRequest): Observable<Contract> {
    return this.http.patch<Contract>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
