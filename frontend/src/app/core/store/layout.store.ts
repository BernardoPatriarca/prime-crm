import { DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { patchState, signalStore, withComputed, withHooks, withMethods, withState } from '@ngrx/signals';

const SIDEBAR_COLLAPSED_KEY = 'prime-crm.sidebar-collapsed';

export const MOBILE_BREAKPOINT_QUERY = '(max-width: 767.98px)';
export const TABLET_BREAKPOINT_QUERY = '(min-width: 768px) and (max-width: 1023.98px)';

export type LayoutBreakpoint = 'mobile' | 'tablet' | 'desktop';

interface LayoutState {
  sidebarCollapsed: boolean;
  breakpoint: LayoutBreakpoint;
  mobileDrawerOpen: boolean;
  tabletSidebarExpanded: boolean;
}

function readStoredCollapsed(): boolean {
  return localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true';
}

const initialState: LayoutState = {
  sidebarCollapsed: false,
  breakpoint: 'desktop',
  mobileDrawerOpen: false,
  tabletSidebarExpanded: false
};

export const LayoutStore = signalStore(
  { providedIn: 'root' },
  withState<LayoutState>(initialState),
  withComputed((store) => ({
    isMobile: computed(() => store.breakpoint() === 'mobile'),
    isTablet: computed(() => store.breakpoint() === 'tablet'),
    isDesktop: computed(() => store.breakpoint() === 'desktop'),
    sidebarDocked: computed(() => store.breakpoint() !== 'mobile'),
    sidebarIconMode: computed(() => {
      switch (store.breakpoint()) {
        case 'mobile':
          return false;
        case 'tablet':
          return !store.tabletSidebarExpanded();
        default:
          return store.sidebarCollapsed();
      }
    }),
    sidebarOverlay: computed(() => store.breakpoint() === 'tablet' && store.tabletSidebarExpanded())
  })),
  withMethods((store) => ({
    setBreakpoint(breakpoint: LayoutBreakpoint): void {
      if (store.breakpoint() === breakpoint) {
        return;
      }
      patchState(store, { breakpoint, mobileDrawerOpen: false, tabletSidebarExpanded: false });
    },
    setSidebarCollapsed(collapsed: boolean): void {
      patchState(store, { sidebarCollapsed: collapsed });
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsed));
    },
    setMobileDrawerOpen(open: boolean): void {
      patchState(store, { mobileDrawerOpen: open });
    },
    closeTransientSidebar(): void {
      patchState(store, { mobileDrawerOpen: false, tabletSidebarExpanded: false });
    },
    toggleSidebar(): void {
      switch (store.breakpoint()) {
        case 'mobile':
          patchState(store, { mobileDrawerOpen: !store.mobileDrawerOpen() });
          return;
        case 'tablet':
          patchState(store, { tabletSidebarExpanded: !store.tabletSidebarExpanded() });
          return;
        default:
          this.setSidebarCollapsed(!store.sidebarCollapsed());
      }
    }
  })),
  withHooks({
    onInit(store) {
      const breakpointObserver = inject(BreakpointObserver);
      const destroyRef = inject(DestroyRef);

      patchState(store, { sidebarCollapsed: readStoredCollapsed() });

      breakpointObserver
        .observe([MOBILE_BREAKPOINT_QUERY, TABLET_BREAKPOINT_QUERY])
        .pipe(takeUntilDestroyed(destroyRef))
        .subscribe((state) => {
          if (state.breakpoints[MOBILE_BREAKPOINT_QUERY]) {
            store.setBreakpoint('mobile');
          } else if (state.breakpoints[TABLET_BREAKPOINT_QUERY]) {
            store.setBreakpoint('tablet');
          } else {
            store.setBreakpoint('desktop');
          }
        });
    }
  })
);
