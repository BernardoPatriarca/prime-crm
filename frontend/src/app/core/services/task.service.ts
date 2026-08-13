import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/page.model';
import { Task, TaskRequest, TaskStatus, TaskStatusUpdateRequest } from '../models/task.model';
import { buildHttpParams } from '../utils/http-params.util';

export interface TaskListQuery {
  search?: string;
  status?: TaskStatus;
  typeId?: string;
  priorityId?: string;
  assignedUserId?: string;
  customerId?: string;
  leadId?: string;
  opportunityId?: string;
  dueFrom?: string;
  dueTo?: string;
  overdue?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/tasks`;

  list(query: TaskListQuery): Observable<PageResponse<Task>> {
    return this.http.get<PageResponse<Task>>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }

  getById(id: string): Observable<Task> {
    return this.http.get<Task>(`${this.baseUrl}/${id}`);
  }

  create(request: TaskRequest): Observable<Task> {
    return this.http.post<Task>(this.baseUrl, request);
  }

  update(id: string, request: TaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/${id}`, request);
  }

  changeStatus(id: string, request: TaskStatusUpdateRequest): Observable<Task> {
    return this.http.patch<Task>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
