import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';

export type ThemeMode = 'light' | 'dark';

const THEME_KEY = 'prime-crm.theme-mode';
const DARK_CLASS = 'app-dark';

interface ThemeState {
  mode: ThemeMode;
}

function readStoredMode(): ThemeMode | null {
  const raw = localStorage.getItem(THEME_KEY);
  return raw === 'light' || raw === 'dark' ? raw : null;
}

function applyMode(mode: ThemeMode): void {
  document.documentElement.classList.toggle(DARK_CLASS, mode === 'dark');
}

function resolveInitialMode(): ThemeMode {
  const stored = readStoredMode();
  if (stored) {
    return stored;
  }
  const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  return prefersDark ? 'dark' : 'light';
}

export const ThemeStore = signalStore(
  { providedIn: 'root' },
  withState<ThemeState>({ mode: 'light' }),
  withComputed((store) => ({
    isDark: computed(() => store.mode() === 'dark')
  })),
  withMethods((store) => ({
    setMode(mode: ThemeMode): void {
      patchState(store, { mode });
      localStorage.setItem(THEME_KEY, mode);
      applyMode(mode);
    },
    toggle(): void {
      const next: ThemeMode = store.mode() === 'dark' ? 'light' : 'dark';
      this.setMode(next);
    },
    initializeFromPreference(mode: ThemeMode): void {
      if (readStoredMode()) {
        return;
      }
      this.setMode(mode);
    }
  })),
  withHooks({
    onInit(store) {
      const initial = resolveInitialMode();
      patchState(store, { mode: initial });
      applyMode(initial);
    }
  })
);
