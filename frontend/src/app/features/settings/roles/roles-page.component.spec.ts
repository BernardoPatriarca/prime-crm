import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { PermissionService } from '../../../core/services/permission.service';
import { RoleService } from '../../../core/services/role.service';
import { RolesPageComponent } from './roles-page.component';

describe('RolesPageComponent', () => {
  let fixture: ComponentFixture<RolesPageComponent>;
  let component: RolesPageComponent;

  const roleServiceStub: Partial<RoleService> = {
    list: () => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  };

  const permissionServiceStub: Partial<PermissionService> = {
    list: () => of([])
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [RolesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: RoleService, useValue: roleServiceStub },
        { provide: PermissionService, useValue: permissionServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RolesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no roles', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a name before saving a new role', () => {
    component['openCreateDialog']();
    fixture.detectChanges();

    component['save']();

    expect(component['form'].controls.name.invalid).toBeTrue();
    expect(component['form'].controls.name.touched).toBeTrue();
  });
});
