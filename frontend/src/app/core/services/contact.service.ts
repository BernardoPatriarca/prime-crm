import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contact, ContactRequest } from '../models/contact.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface ContactListQuery {
  customerId?: string;
  search?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ContactService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/contacts`;

  list(query: ContactListQuery): Observable<PageResponse<Contact>> {
    return this.http.get<PageResponse<Contact>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Contact> {
    return this.http.get<Contact>(`${this.baseUrl}/${id}`);
  }

  create(request: ContactRequest): Observable<Contact> {
    return this.http.post<Contact>(this.baseUrl, request);
  }

  update(id: string, request: ContactRequest): Observable<Contact> {
    return this.http.put<Contact>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
