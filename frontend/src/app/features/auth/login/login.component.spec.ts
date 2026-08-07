import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the username and password fields and the submit button', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('[data-testid="login-username"]')).toBeTruthy();
    expect(compiled.querySelector('[data-testid="login-password"]')).toBeTruthy();
    expect(compiled.querySelector('[data-testid="login-submit"]')).toBeTruthy();
  });

  it('starts with an invalid, empty form', () => {
    expect(component['form'].invalid).toBeTrue();
  });

  it('marks required fields as touched when submitting an empty form', () => {
    component['submit']();
    fixture.detectChanges();

    expect(component['form'].controls.usernameOrEmail.touched).toBeTrue();
    expect(component['form'].controls.password.touched).toBeTrue();
    expect(component['form'].controls.usernameOrEmail.hasError('required')).toBeTrue();
    expect(component['form'].controls.password.hasError('required')).toBeTrue();
  });

  it('becomes valid once both fields are filled', () => {
    component['form'].setValue({ usernameOrEmail: 'admin', password: 'Admin@123' });

    expect(component['form'].valid).toBeTrue();
  });

  function apiError(status: number, errorCode?: string): HttpErrorResponse {
    return new HttpErrorResponse({
      status,
      error: errorCode
        ? { timestamp: '', status, errorCode, message: 'backend message', path: '', fieldErrors: null }
        : null
    });
  }

  it('reports invalid credentials when the API rejects the login with INVALID_CREDENTIALS', () => {
    const message = component['resolveErrorMessage'](apiError(401, 'INVALID_CREDENTIALS'));

    expect(message).toBe('auth.login.invalidCredentials');
  });

  it('reports invalid credentials for any 401 without a recognised error code', () => {
    const message = component['resolveErrorMessage'](apiError(401));

    expect(message).toBe('auth.login.invalidCredentials');
  });

  it('reports a blocked account when the API returns USER_NOT_ACTIVE', () => {
    const message = component['resolveErrorMessage'](apiError(401, 'USER_NOT_ACTIVE'));

    expect(message).toBe('auth.login.userNotActive');
  });

  it('reports a connection failure when the backend is unreachable', () => {
    const message = component['resolveErrorMessage'](apiError(0));

    expect(message).toBe('auth.login.connectionError');
  });

  it('reports a server error for 5xx responses', () => {
    const message = component['resolveErrorMessage'](apiError(500));

    expect(message).toBe('auth.login.serverError');
  });

  it('shows the inline alert and clears the password after a failed login', async () => {
    spyOn(component['sessionStore'], 'login').and.returnValue(
      Promise.reject(apiError(401, 'INVALID_CREDENTIALS'))
    );
    component['form'].setValue({ usernameOrEmail: 'admin', password: 'senhaerrada' });

    await component['submit']();
    fixture.detectChanges();

    expect(component['errorMessage']()).toBe('auth.login.invalidCredentials');
    expect(component['form'].controls.password.value).toBe('');
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="login-error"]')).toBeTruthy();
  });
});
