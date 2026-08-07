import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, LogoutRequest, RefreshTokenRequest } from '../models/auth.model';
import { MeResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auth`;

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request);
  }

  refresh(request: RefreshTokenRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/refresh`, request);
  }

  logout(request: LogoutRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, request);
  }

  me(): Observable<MeResponse> {
    return this.http.get<MeResponse>(`${this.baseUrl}/me`);
  }
}
