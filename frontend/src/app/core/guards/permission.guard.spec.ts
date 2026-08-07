import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { permissionGuard } from './permission.guard';
import { SessionStore } from '../store/session.store';
import { LoginResponse } from '../models/auth.model';

function buildRoute(permission: string | string[]): ActivatedRouteSnapshot {
  return { data: { permission } } as unknown as ActivatedRouteSnapshot;
}

function buildLoginResponse(permissions: string[]): LoginResponse {
  return {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    expiresInSeconds: 3600,
    user: {
      id: 'user-1',
      name: 'Admin',
      email: 'admin@primecrm.com',
      login: 'admin',
      status: 'ACTIVE',
      lastLoginAt: null,
      roles: ['ADMIN'],
      permissions
    }
  };
}

describe('permissionGuard', () => {
  let sessionStore: InstanceType<typeof SessionStore>;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService
      ]
    });
    sessionStore = TestBed.inject(SessionStore);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('allows navigation when the user has the required permission', () => {
    sessionStore.setSession(buildLoginResponse(['USUARIOS_VIEW']));

    const result = TestBed.runInInjectionContext(() =>
      permissionGuard(buildRoute('USUARIOS_VIEW'), {} as RouterStateSnapshot)
    );

    expect(result).toBeTrue();
  });

  it('redirects to /dashboard when the user lacks the required permission', () => {
    sessionStore.setSession(buildLoginResponse(['PIPELINES_VIEW']));

    const result = TestBed.runInInjectionContext(() =>
      permissionGuard(buildRoute('USUARIOS_VIEW'), {} as RouterStateSnapshot)
    ) as UrlTree;

    expect(result instanceof UrlTree).toBeTrue();
    expect(result.toString()).toContain('/dashboard');
  });

  it('allows navigation when no permission is required by the route', () => {
    sessionStore.setSession(buildLoginResponse([]));

    const result = TestBed.runInInjectionContext(() =>
      permissionGuard({ data: {} } as unknown as ActivatedRouteSnapshot, {} as RouterStateSnapshot)
    );

    expect(result).toBeTrue();
  });
});
