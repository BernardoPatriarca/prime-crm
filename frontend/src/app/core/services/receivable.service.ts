import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import {
  GenerateInstallmentsRequest,
  Receivable,
  ReceivablePaymentRequest,
  ReceivableRequest,
  ReceivableStatus
} from '../models/receivable.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface ReceivableListQuery {
  search?: string;
  status?: ReceivableStatus;
  customerId?: string;
  orderId?: string;
  contractId?: string;
  dueFrom?: string;
  dueTo?: string;
  overdue?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ReceivableService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/receivables`;

  list(query: ReceivableListQuery): Observable<PageResponse<Receivable>> {
    return this.http.get<PageResponse<Receivable>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Receivable> {
    return this.http.get<Receivable>(`${this.baseUrl}/${id}`);
  }

  create(request: ReceivableRequest): Observable<Receivable> {
    return this.http.post<Receivable>(this.baseUrl, request);
  }

  generateFromOrder(orderId: string, request: GenerateInstallmentsRequest): Observable<Receivable[]> {
    return this.http.post<Receivable[]>(`${this.baseUrl}/from-order/${orderId}`, request);
  }

  update(id: string, request: ReceivableRequest): Observable<Receivable> {
    return this.http.put<Receivable>(`${this.baseUrl}/${id}`, request);
  }

  pay(id: string, request: ReceivablePaymentRequest): Observable<Receivable> {
    return this.http.patch<Receivable>(`${this.baseUrl}/${id}/pay`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
