import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CalendarEvent, CalendarEventRequest, CalendarEventStatus } from '../models/calendar-event.model';
import { PageResponse } from '../models/page.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface CalendarEventListQuery {
  search?: string;
  status?: CalendarEventStatus;
  typeId?: string;
  assignedUserId?: string;
  customerId?: string;
  leadId?: string;
  opportunityId?: string;
  startFrom?: string;
  startTo?: string;
  overdue?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CalendarEventRangeQuery {
  from: string;
  to: string;
  assignedUserId?: string;
}

@Injectable({ providedIn: 'root' })
export class CalendarEventService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/agenda`;

  list(query: CalendarEventListQuery): Observable<PageResponse<CalendarEvent>> {
    return this.http.get<PageResponse<CalendarEvent>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  range(query: CalendarEventRangeQuery): Observable<CalendarEvent[]> {
    return this.http.get<CalendarEvent[]>(`${this.baseUrl}/range`, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<CalendarEvent> {
    return this.http.get<CalendarEvent>(`${this.baseUrl}/${id}`);
  }

  create(request: CalendarEventRequest): Observable<CalendarEvent> {
    return this.http.post<CalendarEvent>(this.baseUrl, request);
  }

  update(id: string, request: CalendarEventRequest): Observable<CalendarEvent> {
    return this.http.put<CalendarEvent>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
