import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DomainType } from '../models/domain-value.model';

@Injectable({ providedIn: 'root' })
export class DomainTypeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/domain-types`;

  list(): Observable<DomainType[]> {
    return this.http.get<DomainType[]>(this.baseUrl);
  }
}
