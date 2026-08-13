import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { Dashboard } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { SessionStore } from '../../core/store/session.store';
import { DashboardComponent } from './dashboard.component';

const dashboardFixture: Dashboard = {
  from: '2026-07-14',
  to: '2026-08-13',
  generatedAt: '2026-08-13T00:00:00Z',
  metrics: {
    newLeads: 30,
    newLeadsTrend: 50,
    convertedLeads: 6,
    leadConversionRate: 20,
    openOpportunities: 297,
    openAmount: 34146000,
    wonOpportunities: 3,
    wonAmount: 30000,
    wonAmountTrend: -25,
    lostOpportunities: 1,
    winRate: 75,
    averageTicket: 10000,
    activeCustomers: 320,
    newCustomers: 12,
    newCustomersTrend: null
  },
  funnel: {
    pipelineId: 'pipeline-1',
    pipelineName: 'Funil Padrão',
    stages: [
      { name: 'Qualificação', color: '#1E5EFF', displayOrder: 1, count: 40, amount: 400000 },
      { name: 'Proposta', color: '#22C55E', displayOrder: 2, count: 10, amount: 250000 }
    ]
  },
  monthly: Array.from({ length: 12 }, (_, index) => ({
    month: `2026-${String(index + 1).padStart(2, '0')}`,
    openedCount: index,
    openedAmount: index * 1000,
    wonCount: index,
    wonAmount: index * 500
  })),
  ranking: [
    { owner: 'Ana Souza', count: 4, amount: 60000, share: 60 },
    { owner: 'Bruno Lima', count: 2, amount: 40000, share: 40 }
  ],
  tasks: { pending: 5, inProgress: 2, overdue: 3, dueToday: 1, completedThisWeek: 8 }
};

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let dashboardServiceStub: jasmine.SpyObj<DashboardService>;

  async function createComponent(permissions: string[] = ['OPORTUNIDADES_VIEW', 'TAREFAS_VIEW']): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        { provide: DashboardService, useValue: dashboardServiceStub }
      ]
    }).compileComponents();

    TestBed.inject(SessionStore).setSession({
      accessToken: 'token',
      refreshToken: 'refresh',
      expiresInSeconds: 3600,
      tokenType: 'Bearer',
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

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    dashboardServiceStub = jasmine.createSpyObj<DashboardService>('DashboardService', ['load']);
    dashboardServiceStub.load.and.returnValue(of(dashboardFixture));
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
    expect(dashboardServiceStub.load).toHaveBeenCalledTimes(1);
  });

  it('reloads when the period changes', async () => {
    await createComponent();

    component['onPeriodChange'](7);

    expect(component['periodDays']()).toBe(7);
    expect(dashboardServiceStub.load).toHaveBeenCalledTimes(2);
  });

  it('renders one card per metric with the formatted value', async () => {
    await createComponent();

    const cards = component['metricCards']();
    expect(cards.map((card) => card.key)).toEqual([
      'revenue',
      'pipeline',
      'winRate',
      'averageTicket',
      'leads',
      'customers'
    ]);
    expect(cards[0].value).toContain('30.000,00');
    expect(cards[2].value).toBe('75%');
  });

  it('builds a tooltip for every metric', async () => {
    await createComponent();

    const cards = component['metricCards']();
    expect(cards[0].tooltip).toContain('dashboard.tooltips.metric');
    expect(cards.every((card) => card.tooltip.length > 0)).toBeTrue();
  });

  it('describes the trend in words for the tooltip', async () => {
    await createComponent();

    expect(component['trendDescription'](50)).toContain('dashboard.trend.up');
    expect(component['trendDescription'](-25)).toContain('dashboard.trend.down');
    expect(component['trendDescription'](null)).toContain('dashboard.trend.noBaselineHint');
  });

  it('builds a tooltip for each funnel stage and ranking row', async () => {
    await createComponent();

    expect(component['funnelBars']()[0].tooltip).toContain('dashboard.tooltips.funnelStage');
    expect(component['rankingTooltip'](dashboardFixture.ranking[0])).toContain('dashboard.tooltips.ranking');
  });

  it('turns the task summary into five cards with their own variants and tooltips', async () => {
    await createComponent();

    const cards = component['taskCards']();
    expect(cards.map((card) => card.key)).toEqual([
      'overdue',
      'dueToday',
      'pending',
      'inProgress',
      'completedThisWeek'
    ]);
    expect(cards.map((card) => card.variant)).toEqual(['danger', 'warn', 'neutral', 'neutral', 'success']);
    expect(cards.every((card) => card.tooltip.length > 0)).toBeTrue();
  });

  it('shows the trend as positive, negative or without baseline', async () => {
    await createComponent();

    expect(component['trendSeverity'](50)).toBe('success');
    expect(component['trendSeverity'](-25)).toBe('danger');
    expect(component['trendSeverity'](null)).toBe('secondary');
    expect(component['trendLabel'](50)).toBe('+50%');
    expect(component['trendLabel'](-25)).toBe('-25%');
  });

  it('scales the funnel bars against the largest stage and shares against the total', async () => {
    await createComponent();

    const [first, second] = component['funnelBars']();
    expect(first.width).toBe(100);
    expect(second.width).toBe(25);
    expect(first.share).toBe(80);
    expect(second.share).toBe(20);
  });

  it('builds a twelve-point series with month labels', async () => {
    await createComponent();

    const points = component['monthlyPoints']();
    expect(points).toHaveSize(12);
    expect(points[0].label).toBe('jan/26');
    expect(points[11].label).toBe('dez/26');
  });

  it('splits the outcome donut into won, lost and open', async () => {
    await createComponent();

    expect(component['outcomeSegments']().map((segment) => segment.value)).toEqual([3, 1, 297]);
  });

  it('renders the funnel and the ranking in the template', async () => {
    await createComponent();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Funil Padrão');
    expect(compiled.textContent).toContain('Ana Souza');
    expect(compiled.querySelectorAll('.funnel__row').length).toBe(2);
    expect(compiled.querySelectorAll('.ranking__row').length).toBe(2);
  });

  it('hides the pipeline and task panels without the matching permissions', async () => {
    await createComponent([]);
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelectorAll('.funnel__row').length).toBe(0);
    expect(compiled.querySelectorAll('.tasks__item').length).toBe(0);
    expect(compiled.querySelectorAll('.dashboard__metric').length).toBeGreaterThan(0);
  });

  it('shows the error state when the API fails', async () => {
    dashboardServiceStub.load.and.returnValue(throwError(() => new Error('offline')));
    await createComponent();

    expect(component['failed']()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.dashboard__empty')).toBeTruthy();
  });
});
