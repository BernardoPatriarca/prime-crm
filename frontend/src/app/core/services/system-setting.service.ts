import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SystemSetting } from '../models/system-setting.model';

@Injectable({ providedIn: 'root' })
export class SystemSettingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/system-settings`;

  list(): Observable<SystemSetting[]> {
    return this.http.get<SystemSetting[]>(this.baseUrl);
  }

  update(key: string, value: string): Observable<SystemSetting> {
    return this.http.put<SystemSetting>(`${this.baseUrl}/${key}`, { value });
  }
}
