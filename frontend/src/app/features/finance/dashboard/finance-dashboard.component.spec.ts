import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { FinanceDashboard } from '../../../core/models/finance-dashboard.model';
import { FinanceDashboardService } from '../../../core/services/finance-dashboard.service';
import { FinanceDashboardComponent } from './finance-dashboard.component';

const financeDashboardFixture: FinanceDashboard = {
  from: '2026-08-14',
  to: '2026-09-13',
  generatedAt: '2026-09-13T00:00:00Z',
  receivables: {
    openCount: 10,
    openAmount: 50000,
    overdueCount: 2,
    overdueAmount: 8000,
    movementCount: 5,
    movementAmount: 15000,
    movementTrend: 25
  },
  payables: {
    openCount: 6,
    openAmount: 20000,
    overdueCount: 1,
    overdueAmount: 3000,
    movementCount: 3,
    movementAmount: 9000,
    movementTrend: null
  },
  monthly: Array.from({ length: 12 }, (_, index) => ({
    month: `2026-${String(index + 1).padStart(2, '0')}`,
    receivedAmount: index * 1000,
    paidAmount: index * 400,
    netAmount: index * 600
  }))
};

describe('FinanceDashboardComponent', () => {
  let fixture: ComponentFixture<FinanceDashboardComponent>;
  let component: FinanceDashboardComponent;
  let financeDashboardServiceStub: jasmine.SpyObj<FinanceDashboardService>;

  async function createComponent(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [FinanceDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        { provide: FinanceDashboardService, useValue: financeDashboardServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FinanceDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    financeDashboardServiceStub = jasmine.createSpyObj<FinanceDashboardService>('FinanceDashboardService', ['load']);
    financeDashboardServiceStub.load.and.returnValue(of(financeDashboardFixture));
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', async () => {
    await createComponent();
    expect(component).toBeTruthy();
  });

  it('loads the last 30 days by default', async () => {
    await createComponent();

    expect(component['periodDays']()).toBe(30);
    expect(financeDashboardServiceStub.load).toHaveBeenCalledTimes(1);
  });

  it('reloads when the period changes', async () => {
    await createComponent();

    component['onPeriodChange'](7);

    expect(component['periodDays']()).toBe(7);
    expect(financeDashboardServiceStub.load).toHaveBeenCalledTimes(2);
  });

  it('renders one card per metric with the formatted value', async () => {
    await createComponent();

    const cards = component['metricCards']();
    expect(cards.map((card) => card.key)).toEqual([
      'receivableOpen',
      'receivableOverdue',
      'received',
      'payableOpen',
      'payableOverdue',
      'paid'
    ]);
    expect(cards[2].trend).toBe(25);
    expect(cards[5].trend).toBeNull();
  });

  it('builds a tooltip for every metric', async () => {
    await createComponent();

    const cards = component['metricCards']();
    expect(cards.every((card) => card.tooltip.length > 0)).toBeTrue();
  });

  it('builds a twelve-point series with received and paid values', async () => {
    await createComponent();

    const points = component['monthlyPoints']();
    expect(points).toHaveSize(12);
    expect(points[0].label).toBe('jan/26');
    expect(points[11].value).toBe(11000);
    expect(points[11].secondaryValue).toBe(4400);
  });

  it('shows the trend as positive, negative or without baseline', async () => {
    await createComponent();

    expect(component['trendSeverity'](25)).toBe('success');
    expect(component['trendSeverity'](null)).toBe('secondary');
    expect(component['trendLabel'](25)).toBe('+25%');
  });

  it('shows the error state when the API fails', async () => {
    financeDashboardServiceStub.load.and.returnValue(throwError(() => new Error('offline')));
    await createComponent();

    expect(component['failed']()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.finance-dashboard__empty')).toBeTruthy();
  });
});
