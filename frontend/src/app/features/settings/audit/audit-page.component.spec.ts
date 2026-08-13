import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { AuditLog } from '../../../core/models/audit-log.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { AuditPageComponent } from './audit-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function entryFixture(changes: Record<string, unknown> | null): AuditLog {
  return {
    id: 'audit-1',
    entityName: 'Customer',
    entityId: 'customer-1',
    action: 'UPDATE',
    changes,
    userId: 'user-1',
    userEmail: 'admin@primecrm.com',
    ipAddress: '127.0.0.1',
    userAgent: 'Chrome',
    createdAt: '2026-01-01T00:00:00Z'
  };
}

describe('AuditPageComponent', () => {
  let fixture: ComponentFixture<AuditPageComponent>;
  let component: AuditPageComponent;
  let auditLogServiceStub: jasmine.SpyObj<AuditLogService>;

  beforeEach(async () => {
    localStorage.clear();

    auditLogServiceStub = jasmine.createSpyObj<AuditLogService>('AuditLogService', [
      'list',
      'entityNames',
      'timeline',
      'export'
    ]);
    auditLogServiceStub.list.and.returnValue(of(emptyPage));
    auditLogServiceStub.entityNames.and.returnValue(of(['Customer', 'Task']));
    auditLogServiceStub.export.and.returnValue(of(new Blob(['csv'])));

    await TestBed.configureTestingModule({
      imports: [AuditPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        { provide: AuditLogService, useValue: auditLogServiceStub },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the entity names to feed the filter', () => {
    expect(component['entityNames']()).toEqual(['Customer', 'Task']);
  });

  it('splits an update entry into previous and new values', () => {
    component['openDetails'](entryFixture({ name: { old: 'Antigo', new: 'Novo' } }));

    expect(component['changeEntries']()).toEqual([
      { field: 'name', oldValue: 'Antigo', newValue: 'Novo', hasPrevious: true }
    ]);
  });

  it('renders snapshot entries without a previous value', () => {
    component['openDetails'](entryFixture({ login: 'admin' }));

    expect(component['changeEntries']()).toEqual([
      { field: 'login', oldValue: '', newValue: 'admin', hasPrevious: false }
    ]);
  });

  it('sends the selected filters to the API', () => {
    component['actionFilter'].set('DELETE');
    component['entityFilter'].set('Customer');
    component['onFilterChange']();

    const query = auditLogServiceStub.list.calls.mostRecent().args[0];
    expect(query.action).toBe('DELETE');
    expect(query.entityName).toBe('Customer');
  });
});
