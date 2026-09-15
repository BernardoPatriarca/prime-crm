import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ProductivityDashboard, ProductivityDashboardQuery } from '../models/productivity-dashboard.model';
import { buildHttpParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class ProductivityDashboardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard/produtividade`;

  load(query: ProductivityDashboardQuery = {}): Observable<ProductivityDashboard> {
    return this.http.get<ProductivityDashboard>(this.baseUrl, { params: buildHttpParams({ ...query }) });
  }
}
