import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CommercialDashboard, CommercialDashboardQuery } from '../models/commercial-dashboard.model';
import { buildHttpParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class CommercialDashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard/comercial`;

  load(query: CommercialDashboardQuery = {}): Observable<CommercialDashboard> {
    return this.http.get<CommercialDashboard>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }
}
