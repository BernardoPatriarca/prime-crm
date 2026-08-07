import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Holiday, HolidayRequest } from '../models/holiday.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface HolidayListQuery {
  year?: number;
  startDate?: string;
  endDate?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class HolidayService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/holidays`;

  list(query: HolidayListQuery): Observable<PageResponse<Holiday>> {
    return this.http.get<PageResponse<Holiday>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Holiday> {
    return this.http.get<Holiday>(`${this.baseUrl}/${id}`);
  }

  create(request: HolidayRequest): Observable<Holiday> {
    return this.http.post<Holiday>(this.baseUrl, request);
  }

  update(id: string, request: HolidayRequest): Observable<Holiday> {
    return this.http.put<Holiday>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
