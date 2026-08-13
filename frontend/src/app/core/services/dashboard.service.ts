import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Dashboard, DashboardQuery } from '../models/dashboard.model';
import { buildHttpParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard`;

  load(query: DashboardQuery = {}): Observable<Dashboard> {
    return this.http.get<Dashboard>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }
}
