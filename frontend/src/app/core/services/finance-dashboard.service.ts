import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FinanceDashboard, FinanceDashboardQuery } from '../models/finance-dashboard.model';
import { buildHttpParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class FinanceDashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard/financeiro`;

  load(query: FinanceDashboardQuery = {}): Observable<FinanceDashboard> {
    return this.http.get<FinanceDashboard>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }
}
