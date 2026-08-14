import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { providePrimeNG } from 'primeng/config';
import { MessageService, ConfirmationService } from 'primeng/api';
import { App } from './app';
import { LoadingStore } from './core/store/loading.store';
import { PrimeCrmPreset } from './core/theme/prime-crm-preset';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        providePrimeNG({
          theme: {
            preset: PrimeCrmPreset,
            options: { darkModeSelector: '.app-dark' }
          }
        }),
        MessageService,
        ConfirmationService
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('renders the router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });

  it('keeps the loading overlay out of the DOM while nothing is loading', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('[data-testid="global-loading"]')).toBeNull();
  });

  it('shows the centered gif overlay while a request is running', () => {
    const fixture = TestBed.createComponent(App);
    const loadingStore = TestBed.inject(LoadingStore);
    fixture.detectChanges();

    loadingStore.start();
    fixture.detectChanges();

    const overlay = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="global-loading"]');
    expect(overlay).toBeTruthy();
    expect(overlay?.getAttribute('role')).toBe('status');
    expect(overlay?.querySelector('img')?.getAttribute('src')).toBe('loading.gif');
  });

  it('removes the overlay when the last request finishes', () => {
    const fixture = TestBed.createComponent(App);
    const loadingStore = TestBed.inject(LoadingStore);
    loadingStore.start();
    loadingStore.start();
    fixture.detectChanges();

    loadingStore.stop();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="global-loading"]')).toBeTruthy();

    loadingStore.stop();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="global-loading"]')).toBeNull();
  });
});
