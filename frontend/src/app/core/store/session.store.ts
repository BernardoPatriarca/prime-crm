import { computed, inject } from '@angular/core';
import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { TokenStorageService } from '../services/token-storage.service';
import { LoginRequest, LoginResponse } from '../models/auth.model';
import { MeResponse } from '../models/user.model';

const SESSION_USER_KEY = 'prime-crm.session-user';

interface SessionState {
  user: MeResponse | null;
  accessToken: string | null;
  refreshToken: string | null;
}

function readStoredUser(): MeResponse | null {
  const raw = localStorage.getItem(SESSION_USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as MeResponse;
  } catch {
    return null;
  }
}

const initialState: SessionState = {
  user: null,
  accessToken: null,
  refreshToken: null
};

export const SessionStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    isAuthenticated: computed(() => !!store.accessToken() && !!store.user())
  })),
  withMethods((store, authService = inject(AuthService), tokenStorage = inject(TokenStorageService)) => ({
    hasPermission(code: string | string[]): boolean {
      const permissions = store.user()?.permissions ?? [];
      const codes = Array.isArray(code) ? code : [code];
      return codes.some((c) => permissions.includes(c));
    },
    setSession(response: LoginResponse): void {
      tokenStorage.setTokens(response.accessToken, response.refreshToken);
      localStorage.setItem(SESSION_USER_KEY, JSON.stringify(response.user));
      patchState(store, {
        user: response.user,
        accessToken: response.accessToken,
        refreshToken: response.refreshToken
      });
    },
    async login(request: LoginRequest): Promise<void> {
      const response = await firstValueFrom(authService.login(request));
      this.setSession(response);
    },
    async logout(): Promise<void> {
      const refreshToken = store.refreshToken();
      patchState(store, { user: null, accessToken: null, refreshToken: null });
      tokenStorage.clear();
      localStorage.removeItem(SESSION_USER_KEY);
      if (refreshToken) {
        try {
          await firstValueFrom(authService.logout({ refreshToken }));
        } catch {
          Promise.resolve();
        }
      }
    },
    clearLocalSession(): void {
      patchState(store, { user: null, accessToken: null, refreshToken: null });
      tokenStorage.clear();
      localStorage.removeItem(SESSION_USER_KEY);
    }
  })),
  withHooks({
    onInit(store) {
      const tokenStorage = inject(TokenStorageService);
      const accessToken = tokenStorage.getAccessToken();
      const refreshToken = tokenStorage.getRefreshToken();
      const user = readStoredUser();
      if (accessToken && refreshToken && user) {
        patchState(store, { accessToken, refreshToken, user });
      }
    }
  })
);
