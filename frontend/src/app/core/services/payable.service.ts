import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { Payable, PayablePaymentRequest, PayableRequest, PayableStatus } from '../models/payable.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface PayableListQuery {
  search?: string;
  status?: PayableStatus;
  supplierId?: string;
  categoryId?: string;
  dueFrom?: string;
  dueTo?: string;
  overdue?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class PayableService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/payables`;

  list(query: PayableListQuery): Observable<PageResponse<Payable>> {
    return this.http.get<PageResponse<Payable>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Payable> {
    return this.http.get<Payable>(`${this.baseUrl}/${id}`);
  }

  create(request: PayableRequest): Observable<Payable> {
    return this.http.post<Payable>(this.baseUrl, request);
  }

  update(id: string, request: PayableRequest): Observable<Payable> {
    return this.http.put<Payable>(`${this.baseUrl}/${id}`, request);
  }

  pay(id: string, request: PayablePaymentRequest): Observable<Payable> {
    return this.http.patch<Payable>(`${this.baseUrl}/${id}/pay`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
