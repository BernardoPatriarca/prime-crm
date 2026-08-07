import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { SessionStore } from '../store/session.store';
import { LoginResponse } from '../models/auth.model';

const mockLoginResponse: LoginResponse = {
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
    permissions: []
  }
};

describe('authGuard', () => {
  let sessionStore: InstanceType<typeof SessionStore>;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    sessionStore = TestBed.inject(SessionStore);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('allows navigation when the user is authenticated', () => {
    sessionStore.setSession(mockLoginResponse);

    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/dashboard' } as RouterStateSnapshot)
    );

    expect(result).toBeTrue();
  });

  it('redirects to /login when the user is not authenticated', () => {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/configuracoes/usuarios' } as RouterStateSnapshot)
    ) as UrlTree;

    const router = TestBed.inject(Router);
    expect(result instanceof UrlTree).toBeTrue();
    expect(router.serializeUrl(result)).toContain('/login');
  });
});
