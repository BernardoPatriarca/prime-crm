import { TestBed } from '@angular/core/testing';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { Subject } from 'rxjs';
import {
  LayoutStore,
  MOBILE_BREAKPOINT_QUERY,
  TABLET_BREAKPOINT_QUERY
} from './layout.store';

function breakpointState(matched: string | null): BreakpointState {
  return {
    matches: matched !== null,
    breakpoints: {
      [MOBILE_BREAKPOINT_QUERY]: matched === MOBILE_BREAKPOINT_QUERY,
      [TABLET_BREAKPOINT_QUERY]: matched === TABLET_BREAKPOINT_QUERY
    }
  };
}

describe('LayoutStore', () => {
  let store: InstanceType<typeof LayoutStore>;
  let breakpoints$: Subject<BreakpointState>;

  beforeEach(() => {
    localStorage.clear();
    breakpoints$ = new Subject<BreakpointState>();
    TestBed.configureTestingModule({
      providers: [
        {
          provide: BreakpointObserver,
          useValue: { observe: () => breakpoints$.asObservable() }
        }
      ]
    });
    store = TestBed.inject(LayoutStore);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('starts in desktop mode with the drawer closed', () => {
    expect(store.breakpoint()).toBe('desktop');
    expect(store.isDesktop()).toBeTrue();
    expect(store.mobileDrawerOpen()).toBeFalse();
  });

  it('switches to mobile mode below 768px with the drawer closed', () => {
    breakpoints$.next(breakpointState(MOBILE_BREAKPOINT_QUERY));

    expect(store.breakpoint()).toBe('mobile');
    expect(store.isMobile()).toBeTrue();
    expect(store.sidebarDocked()).toBeFalse();
    expect(store.mobileDrawerOpen()).toBeFalse();
  });

  it('switches to tablet mode between 768px and 1023px with the sidebar in icon mode', () => {
    breakpoints$.next(breakpointState(TABLET_BREAKPOINT_QUERY));

    expect(store.breakpoint()).toBe('tablet');
    expect(store.isTablet()).toBeTrue();
    expect(store.sidebarDocked()).toBeTrue();
    expect(store.sidebarIconMode()).toBeTrue();
    expect(store.sidebarOverlay()).toBeFalse();
  });

  it('switches back to desktop mode when no media query matches', () => {
    breakpoints$.next(breakpointState(MOBILE_BREAKPOINT_QUERY));
    breakpoints$.next(breakpointState(null));

    expect(store.breakpoint()).toBe('desktop');
    expect(store.isDesktop()).toBeTrue();
  });

  it('toggles the mobile drawer instead of the collapsed preference on mobile', () => {
    breakpoints$.next(breakpointState(MOBILE_BREAKPOINT_QUERY));

    store.toggleSidebar();

    expect(store.mobileDrawerOpen()).toBeTrue();
    expect(store.sidebarCollapsed()).toBeFalse();
    expect(localStorage.getItem('prime-crm.sidebar-collapsed')).toBeNull();

    store.toggleSidebar();

    expect(store.mobileDrawerOpen()).toBeFalse();
  });

  it('expands the tablet sidebar as an overlay without persisting the preference', () => {
    breakpoints$.next(breakpointState(TABLET_BREAKPOINT_QUERY));

    store.toggleSidebar();

    expect(store.sidebarIconMode()).toBeFalse();
    expect(store.sidebarOverlay()).toBeTrue();
    expect(localStorage.getItem('prime-crm.sidebar-collapsed')).toBeNull();
  });

  it('closes transient panels on navigation', () => {
    breakpoints$.next(breakpointState(MOBILE_BREAKPOINT_QUERY));
    store.toggleSidebar();

    store.closeTransientSidebar();

    expect(store.mobileDrawerOpen()).toBeFalse();
    expect(store.sidebarOverlay()).toBeFalse();
  });

  it('closes transient panels when the breakpoint changes', () => {
    breakpoints$.next(breakpointState(MOBILE_BREAKPOINT_QUERY));
    store.toggleSidebar();
    expect(store.mobileDrawerOpen()).toBeTrue();

    breakpoints$.next(breakpointState(TABLET_BREAKPOINT_QUERY));

    expect(store.mobileDrawerOpen()).toBeFalse();
    expect(store.sidebarOverlay()).toBeFalse();
  });

  it('persists the collapsed preference on desktop and reflects it in icon mode', () => {
    store.toggleSidebar();

    expect(store.sidebarCollapsed()).toBeTrue();
    expect(store.sidebarIconMode()).toBeTrue();
    expect(localStorage.getItem('prime-crm.sidebar-collapsed')).toBe('true');
  });

  it('keeps the desktop collapsed preference while browsing on mobile', () => {
    store.setSidebarCollapsed(true);
    breakpoints$.next(breakpointState(MOBILE_BREAKPOINT_QUERY));

    expect(store.sidebarIconMode()).toBeFalse();
    expect(store.sidebarCollapsed()).toBeTrue();

    breakpoints$.next(breakpointState(null));

    expect(store.sidebarIconMode()).toBeTrue();
  });
});
