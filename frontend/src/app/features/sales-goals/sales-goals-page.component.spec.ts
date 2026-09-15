import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { SalesGoal } from '../../core/models/sales-goal.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { SalesGoalService } from '../../core/services/sales-goal.service';
import { SalesGoalsPageComponent } from './sales-goals-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function goalFixture(overrides: Partial<SalesGoal> = {}): SalesGoal {
  return {
    id: 'goal-1',
    owner: { id: 'user-1', name: 'Ana', email: 'ana@primecrm.com' },
    referenceMonth: '2026-09-01',
    targetAmount: 10000,
    realizedAmount: 2500,
    achievementPercent: 25,
    notes: null,
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
    ...overrides
  };
}

describe('SalesGoalsPageComponent', () => {
  let fixture: ComponentFixture<SalesGoalsPageComponent>;
  let component: SalesGoalsPageComponent;
  let salesGoalServiceStub: jasmine.SpyObj<SalesGoalService>;

  beforeEach(async () => {
    localStorage.clear();

    salesGoalServiceStub = jasmine.createSpyObj<SalesGoalService>('SalesGoalService', [
      'list',
      'create',
      'update',
      'delete'
    ]);
    salesGoalServiceStub.list.and.returnValue(of(emptyPage));

    await TestBed.configureTestingModule({
      imports: [SalesGoalsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: SalesGoalService, useValue: salesGoalServiceStub },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage), getById: () => of(null) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SalesGoalsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no goals', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires an owner before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.ownerUserId.invalid).toBeTrue();
    expect(salesGoalServiceStub.create).not.toHaveBeenCalled();
  });

  it('formats the reference month as MM/YYYY', () => {
    expect(component['formatMonth']('2026-09-01')).toBe('09/2026');
  });

  it('classifies achievement severity by percent thresholds', () => {
    expect(component['achievementSeverity'](10)).toBe('danger');
    expect(component['achievementSeverity'](60)).toBe('warn');
    expect(component['achievementSeverity'](100)).toBe('success');
  });

  it('loads the linked owner of a goal when editing', () => {
    component['openEditDialog'](goalFixture());

    expect(component['form'].controls.ownerUserId.value).toBe('user-1');
    expect(component['form'].controls.referenceMonth.value).toBeInstanceOf(Date);
  });
});
