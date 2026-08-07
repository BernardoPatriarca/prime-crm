import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { SessionStore } from './session.store';
import { LoginResponse } from '../models/auth.model';

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

describe('SessionStore', () => {
  let store: InstanceType<typeof SessionStore>;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    store = TestBed.inject(SessionStore);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('starts unauthenticated when there is no stored session', () => {
    expect(store.isAuthenticated()).toBeFalse();
    expect(store.user()).toBeNull();
  });

  it('becomes authenticated after setSession', () => {
    store.setSession(buildLoginResponse(['USUARIOS_VIEW']));

    expect(store.isAuthenticated()).toBeTrue();
    expect(store.accessToken()).toBe('access-token');
  });

  it('hasPermission returns true only for granted permission codes', () => {
    store.setSession(buildLoginResponse(['USUARIOS_VIEW', 'PIPELINES_VIEW']));

    expect(store.hasPermission('USUARIOS_VIEW')).toBeTrue();
    expect(store.hasPermission('PERFIS_VIEW')).toBeFalse();
  });

  it('hasPermission returns true when any of the given codes is granted', () => {
    store.setSession(buildLoginResponse(['PIPELINES_VIEW']));

    expect(store.hasPermission(['PERFIS_VIEW', 'PIPELINES_VIEW'])).toBeTrue();
    expect(store.hasPermission(['PERFIS_VIEW', 'PERMISSOES_VIEW'])).toBeFalse();
  });

  it('hasPermission returns false once the session is cleared', () => {
    store.setSession(buildLoginResponse(['USUARIOS_VIEW']));
    store.clearLocalSession();

    expect(store.isAuthenticated()).toBeFalse();
    expect(store.hasPermission('USUARIOS_VIEW')).toBeFalse();
  });
});
