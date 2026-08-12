import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contact } from '../models/contact.model';
import { Customer, CustomerRequest, PersonType } from '../models/customer.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface CustomerListQuery {
  search?: string;
  personType?: PersonType;
  clientTypeId?: string;
  segmentId?: string;
  ownerUserId?: string;
  active?: boolean;
  tagIds?: string[];
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/customers`;

  list(query: CustomerListQuery): Observable<PageResponse<Customer>> {
    const { tagIds, ...scalars } = query;
    let params: HttpParams = buildHttpParams({ ...scalars });
    for (const tagId of tagIds ?? []) {
      params = params.append('tagIds', tagId);
    }
    return this.http.get<PageResponse<Customer>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }

  contacts(id: string): Observable<Contact[]> {
    return this.http.get<Contact[]>(`${this.baseUrl}/${id}/contacts`);
  }

  create(request: CustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, request);
  }

  update(id: string, request: CustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
