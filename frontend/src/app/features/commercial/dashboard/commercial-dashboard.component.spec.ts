import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { CommercialDashboard } from '../../../core/models/commercial-dashboard.model';
import { CommercialDashboardService } from '../../../core/services/commercial-dashboard.service';
import { CommercialDashboardComponent } from './commercial-dashboard.component';

const commercialDashboardFixture: CommercialDashboard = {
  from: '2026-08-14',
  to: '2026-09-13',
  generatedAt: '2026-09-13T00:00:00Z',
  proposals: { totalCount: 4, totalAmount: 40000, closedCount: 1, closedAmount: 10000, conversionRate: 25 },
  orders: { totalCount: 5, totalAmount: 50000, closedCount: 2, closedAmount: 20000, conversionRate: 40 },
  contracts: { activeCount: 10, activeRecurringAmount: 5000, expiringCount: 2, expiringAmount: 800 },
  monthly: Array.from({ length: 12 }, (_, index) => ({
    month: `2026-${String(index + 1).padStart(2, '0')}`,
    proposalsAmount: index * 1000,
    ordersAmount: index * 400
  }))
};

describe('CommercialDashboardComponent', () => {
  let fixture: ComponentFixture<CommercialDashboardComponent>;
  let component: CommercialDashboardComponent;
  let commercialDashboardServiceStub: jasmine.SpyObj<CommercialDashboardService>;

  async function createComponent(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [CommercialDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        { provide: CommercialDashboardService, useValue: commercialDashboardServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CommercialDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    commercialDashboardServiceStub = jasmine.createSpyObj<CommercialDashboardService>('CommercialDashboardService', [
      'load'
    ]);
    commercialDashboardServiceStub.load.and.returnValue(of(commercialDashboardFixture));
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
    expect(commercialDashboardServiceStub.load).toHaveBeenCalledTimes(1);
  });

  it('reloads when the period changes', async () => {
    await createComponent();

    component['onPeriodChange'](90);

    expect(component['periodDays']()).toBe(90);
    expect(commercialDashboardServiceStub.load).toHaveBeenCalledTimes(2);
  });

  it('renders one card per metric', async () => {
    await createComponent();

    const cards = component['metricCards']();
    expect(cards.map((card) => card.key)).toEqual([
      'proposalsTotal',
      'proposalsConversion',
      'ordersTotal',
      'ordersConversion',
      'contractsActive',
      'contractsExpiring'
    ]);
    expect(cards[1].value).toBe('25%');
    expect(cards[3].value).toBe('40%');
  });

  it('builds a tooltip for every metric', async () => {
    await createComponent();

    const cards = component['metricCards']();
    expect(cards.every((card) => card.tooltip.length > 0)).toBeTrue();
  });

  it('builds a twelve-point series with proposals and orders amounts', async () => {
    await createComponent();

    const points = component['monthlyPoints']();
    expect(points).toHaveSize(12);
    expect(points[0].label).toBe('jan/26');
    expect(points[11].value).toBe(11000);
    expect(points[11].secondaryValue).toBe(4400);
  });

  it('shows the error state when the API fails', async () => {
    commercialDashboardServiceStub.load.and.returnValue(throwError(() => new Error('offline')));
    await createComponent();

    expect(component['failed']()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.commercial-dashboard__empty')).toBeTruthy();
  });
});
