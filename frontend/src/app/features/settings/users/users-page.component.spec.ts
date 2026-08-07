import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { RoleService } from '../../../core/services/role.service';
import { UsersPageComponent } from './users-page.component';

describe('UsersPageComponent', () => {
  let fixture: ComponentFixture<UsersPageComponent>;
  let component: UsersPageComponent;

  const adminUserServiceStub: Partial<AdminUserService> = {
    list: () => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  };

  const roleServiceStub: Partial<RoleService> = {
    list: () => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [UsersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: AdminUserService, useValue: adminUserServiceStub },
        { provide: RoleService, useValue: roleServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UsersPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no users', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a password when creating a new user but not when editing', () => {
    component['openCreateDialog']();
    expect(component['form'].controls.password.hasValidator).toBeTruthy();
    component['form'].controls.password.setValue('');
    component['save']();
    expect(component['form'].controls.password.invalid).toBeTrue();

    component['openEditDialog']({
      id: '1',
      name: 'Ana',
      email: 'ana@example.com',
      login: 'ana',
      phone: null,
      status: 'ACTIVE',
      lastLoginAt: null,
      createdAt: new Date().toISOString(),
      roles: []
    });
    expect(component['form'].controls.password.valid).toBeTrue();
  });

  it('flags mismatched passwords in the reset password form', () => {
    component['openResetPasswordDialog']({
      id: '1',
      name: 'Ana',
      email: 'ana@example.com',
      login: 'ana',
      phone: null,
      status: 'ACTIVE',
      lastLoginAt: null,
      createdAt: new Date().toISOString(),
      roles: []
    });
    component['resetPasswordForm'].setValue({ newPassword: 'Password123', confirmPassword: 'Different123' });

    expect(component['resetPasswordForm'].errors?.['passwordMismatch']).toBeTrue();
  });
});
