import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Report, ReportKey, ReportQuery } from '../models/report.model';
import { buildHttpParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/reports`;

  load(report: ReportKey, query: ReportQuery): Observable<Report> {
    return this.http.get<Report>(`${this.baseUrl}/${report}`, { params: buildHttpParams({ ...query }) });
  }

  export(report: ReportKey, query: ReportQuery): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${report}/export`, {
      params: buildHttpParams({ ...query }),
      responseType: 'blob'
    });
  }
}
