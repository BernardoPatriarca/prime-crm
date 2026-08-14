import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { NotificationList } from '../../core/models/notification.model';
import { GlobalSearch } from '../../core/models/search.model';
import { AuthService } from '../../core/services/auth.service';
import { GlobalSearchService } from '../../core/services/global-search.service';
import { NotificationService } from '../../core/services/notification.service';
import { SessionStore } from '../../core/store/session.store';
import { TopbarComponent } from './topbar.component';

const notifications: NotificationList = {
  total: 3,
  items: [
    {
      type: 'TASK_OVERDUE',
      severity: 'DANGER',
      referenceId: 'task-1',
      title: 'Ligar para o cliente',
      description: 'Cliente Alfa',
      link: '/tarefas',
      date: '2026-08-01T12:00:00Z'
    },
    {
      type: 'LEAD_WITHOUT_OWNER',
      severity: 'INFO',
      referenceId: 'lead-1',
      title: 'Lead sem dono',
      description: null,
      link: '/leads',
      date: null
    }
  ]
};

const searchResponse: GlobalSearch = {
  query: 'alfa',
  total: 1,
  results: [
    {
      type: 'CUSTOMER',
      id: 'customer-1',
      code: 'CLI-000001',
      title: 'Cliente Alfa',
      subtitle: 'Alfa Ltda',
      link: '/clientes'
    }
  ]
};

describe('TopbarComponent', () => {
  let fixture: ComponentFixture<TopbarComponent>;
  let component: TopbarComponent;
  let notificationServiceStub: jasmine.SpyObj<NotificationService>;
  let searchServiceStub: jasmine.SpyObj<GlobalSearchService>;
  let authServiceStub: jasmine.SpyObj<AuthService>;

  function seedSession(permissions: string[]): void {
    TestBed.inject(SessionStore).setSession({
      accessToken: 'token',
      refreshToken: 'refresh',
      tokenType: 'Bearer',
      expiresInSeconds: 3600,
      user: {
        id: 'user-1',
        name: 'Administrador Prime',
        email: 'admin@primecrm.local',
        login: 'admin',
        status: 'ACTIVE',
        lastLoginAt: null,
        roles: ['Administrador'],
        permissions
      }
    });
  }

  beforeEach(async () => {
    localStorage.clear();

    notificationServiceStub = jasmine.createSpyObj<NotificationService>('NotificationService', ['list']);
    notificationServiceStub.list.and.returnValue(of(notifications));
    searchServiceStub = jasmine.createSpyObj<GlobalSearchService>('GlobalSearchService', ['search']);
    searchServiceStub.search.and.returnValue(of(searchResponse));
    authServiceStub = jasmine.createSpyObj<AuthService>('AuthService', ['changeOwnPassword', 'logout', 'me']);
    authServiceStub.changeOwnPassword.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [TopbarComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        { provide: NotificationService, useValue: notificationServiceStub },
        { provide: GlobalSearchService, useValue: searchServiceStub },
        { provide: AuthService, useValue: authServiceStub }
      ]
    }).compileComponents();

    seedSession(['TAREFAS_CREATE', 'CLIENTES_CREATE']);

    fixture = TestBed.createComponent(TopbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the notifications of the logged user on startup', () => {
    expect(notificationServiceStub.list).toHaveBeenCalled();
    expect(component['notifications']()).toHaveSize(2);
    expect(component['badgeValue']()).toBe('3');
  });

  it('caps the badge at 99+', () => {
    component['notificationTotal'].set(1200);

    expect(component['badgeValue']()).toBe('99+');
  });

  it('keeps working when the notification endpoint fails', () => {
    notificationServiceStub.list.and.returnValue(throwError(() => new Error('offline')));

    component['loadNotifications']();

    expect(component['notificationsLoading']()).toBeFalse();
  });

  it('does not search with less than two characters', fakeAsync(() => {
    searchServiceStub.search.calls.reset();

    component['onSearchInput']('a');
    tick(500);

    expect(searchServiceStub.search).not.toHaveBeenCalled();
    expect(component['searchResults']()).toEqual([]);
  }));

  it('searches with debounce and keeps the results', fakeAsync(() => {
    component['onSearchInput']('alfa');
    tick(500);

    expect(searchServiceStub.search).toHaveBeenCalledWith('alfa');
    expect(component['searchResults']()).toHaveSize(1);
  }));

  it('clears the search when navigating to a result', async () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);

    await component['goTo']('/clientes');

    expect(router.navigateByUrl).toHaveBeenCalledWith('/clientes');
    expect(component['searchTerm']()).toBe('');
    expect(component['searchResults']()).toEqual([]);
  });

  it('enables only the creation shortcuts the user has permission for', () => {
    const items = component['newButtonItems']();

    expect(items.find((item) => item.icon === 'pi pi-check-square')?.disabled).toBeFalse();
    expect(items.find((item) => item.icon === 'pi pi-bullseye')?.disabled).toBeTrue();
  });

  it('opens the creation route with the novo flag', async () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    await component['openDefaultCreation']();

    expect(router.navigate).toHaveBeenCalledWith(['/clientes'], { queryParams: { novo: 1 } });
  });

  it('refuses to submit the password form when the confirmation does not match', () => {
    component['openPasswordDialog']();
    component['passwordForm'].setValue({
      currentPassword: 'Admin@123',
      newPassword: 'NovaSenha1',
      confirmPassword: 'Diferente1'
    });

    component['savePassword']();

    expect(component['passwordMismatch']).toBeTrue();
    expect(authServiceStub.changeOwnPassword).not.toHaveBeenCalled();
  });

  it('changes the password and closes the dialog', () => {
    component['openPasswordDialog']();
    component['passwordForm'].setValue({
      currentPassword: 'Admin@123',
      newPassword: 'NovaSenha1',
      confirmPassword: 'NovaSenha1'
    });

    component['savePassword']();

    expect(authServiceStub.changeOwnPassword).toHaveBeenCalledWith({
      currentPassword: 'Admin@123',
      newPassword: 'NovaSenha1'
    });
    expect(component['passwordDialogVisible']()).toBeFalse();
  });
});
