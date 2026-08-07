import { computed } from '@angular/core';
import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';

interface LoadingState {
  activeRequests: number;
}

export const LoadingStore = signalStore(
  { providedIn: 'root' },
  withState<LoadingState>({ activeRequests: 0 }),
  withComputed((store) => ({
    isLoading: computed(() => store.activeRequests() > 0)
  })),
  withMethods((store) => ({
    start(): void {
      patchState(store, { activeRequests: store.activeRequests() + 1 });
    },
    stop(): void {
      patchState(store, { activeRequests: Math.max(0, store.activeRequests() - 1) });
    }
  }))
);
