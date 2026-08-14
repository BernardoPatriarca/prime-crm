import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GlobalSearch } from '../models/search.model';
import { buildHttpParams } from '../utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class GlobalSearchService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/search`;

  search(query: string): Observable<GlobalSearch> {
    return this.http.get<GlobalSearch>(this.baseUrl, { params: buildHttpParams({ query }) });
  }
}
