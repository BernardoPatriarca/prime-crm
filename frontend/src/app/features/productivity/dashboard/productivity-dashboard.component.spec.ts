import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { ProductivityDashboard } from '../../../core/models/productivity-dashboard.model';
import { ProductivityDashboardService } from '../../../core/services/productivity-dashboard.service';
import { ProductivityDashboardComponent } from './productivity-dashboard.component';

const productivityDashboardFixture: ProductivityDashboard = {
  from: '2026-08-14',
  to: '2026-09-13',
  generatedAt: '2026-09-13T00:00:00Z',
  tasks: { pending: 5, inProgress: 2, overdue: 3, dueToday: 1, completedThisWeek: 8 },
  taskRanking: [
    { owner: 'Ana Souza', completedCount: 3, share: 75 },
    { owner: 'Bruno Lima', completedCount: 1, share: 25 }
  ],
  agenda: { scheduledToday: 4, scheduledThisWeek: 12, overdue: 2 }
};

describe('ProductivityDashboardComponent', () => {
  let fixture: ComponentFixture<ProductivityDashboardComponent>;
  let component: ProductivityDashboardComponent;
  let productivityDashboardServiceStub: jasmine.SpyObj<ProductivityDashboardService>;

  async function createComponent(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ProductivityDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        { provide: ProductivityDashboardService, useValue: productivityDashboardServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductivityDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    localStorage.clear();
    productivityDashboardServiceStub = jasmine.createSpyObj<ProductivityDashboardService>(
      'ProductivityDashboardService',
      ['load']
    );
    productivityDashboardServiceStub.load.and.returnValue(of(productivityDashboardFixture));
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
    expect(productivityDashboardServiceStub.load).toHaveBeenCalledTimes(1);
  });

  it('reloads when the period changes', async () => {
    await createComponent();

    component['onPeriodChange'](7);

    expect(component['periodDays']()).toBe(7);
    expect(productivityDashboardServiceStub.load).toHaveBeenCalledTimes(2);
  });

  it('turns the task summary into five cards with their own variants', async () => {
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

  it('turns the agenda summary into three cards', async () => {
    await createComponent();

    const cards = component['agendaCards']();
    expect(cards.map((card) => card.value)).toEqual([4, 12, 2]);
  });

  it('builds a tooltip for each ranking row', async () => {
    await createComponent();

    expect(component['rankingTooltip'](productivityDashboardFixture.taskRanking[0]))
      .toContain('productivityDashboard.tooltips.ranking');
  });

  it('renders the ranking rows in the template', async () => {
    await createComponent();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.textContent).toContain('Ana Souza');
    expect(compiled.querySelectorAll('.ranking__row').length).toBe(2);
  });

  it('shows the error state when the API fails', async () => {
    productivityDashboardServiceStub.load.and.returnValue(throwError(() => new Error('offline')));
    await createComponent();

    expect(component['failed']()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.productivity-dashboard__empty')).toBeTruthy();
  });
});
