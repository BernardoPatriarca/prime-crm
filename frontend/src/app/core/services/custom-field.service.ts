import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CustomField, CustomFieldRequest } from '../models/custom-field.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface CustomFieldListQuery {
  targetEntity?: string;
  search?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class CustomFieldService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/custom-fields`;

  list(query: CustomFieldListQuery): Observable<PageResponse<CustomField>> {
    return this.http.get<PageResponse<CustomField>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<CustomField> {
    return this.http.get<CustomField>(`${this.baseUrl}/${id}`);
  }

  create(request: CustomFieldRequest): Observable<CustomField> {
    return this.http.post<CustomField>(this.baseUrl, request);
  }

  update(id: string, request: CustomFieldRequest): Observable<CustomField> {
    return this.http.put<CustomField>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
