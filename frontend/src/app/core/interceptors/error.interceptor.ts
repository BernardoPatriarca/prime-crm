import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiErrorResponse } from '../models/api-error.model';
import { LoginResponse } from '../models/auth.model';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { SessionStore } from '../store/session.store';

let refreshInFlight: Observable<LoginResponse> | null = null;

const AUTH_ENDPOINTS = ['/auth/login', '/auth/refresh', '/auth/logout'];

function isAuthEndpoint(url: string): boolean {
  return AUTH_ENDPOINTS.some((endpoint) => url.includes(endpoint));
}

function isLoginEndpoint(url: string): boolean {
  return url.includes('/auth/login');
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const messageService = inject(MessageService);
  const translate = inject(TranslateService);
  const authService = inject(AuthService);
  const tokenStorage = inject(TokenStorageService);
  const sessionStore = inject(SessionStore);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      if (isLoginEndpoint(req.url)) {
        return throwError(() => error);
      }

      if (error.status === 0) {
        messageService.add({
          severity: 'error',
          summary: translate.instant('common.errors.networkTitle'),
          detail: translate.instant('common.errors.networkMessage')
        });
        return throwError(() => error);
      }

      if (error.status === 401 && !isAuthEndpoint(req.url)) {
        const refreshToken = tokenStorage.getRefreshToken();

        if (!refreshToken) {
          sessionStore.clearLocalSession();
          router.navigate(['/login']);
          return throwError(() => error);
        }

        if (!refreshInFlight) {
          refreshInFlight = authService.refresh({ refreshToken });
        }

        return refreshInFlight.pipe(
          switchMap((response) => {
            refreshInFlight = null;
            sessionStore.setSession(response);
            const retried = req.clone({
              setHeaders: { Authorization: `Bearer ${response.accessToken}` }
            });
            return next(retried);
          }),
          catchError((refreshError: unknown) => {
            refreshInFlight = null;
            sessionStore.clearLocalSession();
            router.navigate(['/login']);
            return throwError(() => refreshError);
          })
        );
      }

      const apiError = error.error as ApiErrorResponse | undefined;
      const detail = apiError?.message || translate.instant('common.errors.genericMessage');

      messageService.add({
        severity: 'error',
        summary: translate.instant('common.errors.genericTitle'),
        detail
      });

      return throwError(() => error);
    })
  );
};
