import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { FinanceDashboard } from '../../../core/models/finance-dashboard.model';
import { FinanceDashboardService } from '../../../core/services/finance-dashboard.service';
import { AreaChartComponent, AreaChartPoint } from '../../../shared/components/charts/area-chart.component';
import { formatCurrencyBRL } from '../../../shared/utils/format.util';

const PERIOD_OPTIONS = [7, 30, 90] as const;
const MONTH_LABELS = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

interface PeriodOption {
  label: string;
  value: number;
}

interface MetricCard {
  key: string;
  icon: string;
  value: string;
  hint: string;
  tooltip: string;
  trend: number | null;
  accent: string;
}

function isoDaysAgo(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

@Component({
  selector: 'app-finance-dashboard',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    TranslatePipe,
    CardModule,
    ButtonModule,
    SelectButtonModule,
    SkeletonModule,
    TagModule,
    TooltipModule,
    AreaChartComponent
  ],
  templateUrl: './finance-dashboard.component.html',
  styleUrl: './finance-dashboard.component.scss'
})
export class FinanceDashboardComponent {
  private readonly financeDashboardService = inject(FinanceDashboardService);
  private readonly translate = inject(TranslateService);

  protected readonly data = signal<FinanceDashboard | null>(null);
  protected readonly loading = signal(false);
  protected readonly failed = signal(false);
  protected readonly periodDays = signal<number>(30);

  protected readonly periodOptions = computed<PeriodOption[]>(() => {
    this.translate.currentLang();
    return PERIOD_OPTIONS.map((days) => ({
      label: this.translate.instant('dashboard.period.days', { days }),
      value: days
    }));
  });

  protected readonly metricCards = computed<MetricCard[]>(() => {
    const dashboard = this.data();
    if (!dashboard) {
      return [];
    }
    const t = (key: string, params?: Record<string, unknown>) =>
      this.translate.instant(`financeDashboard.metrics.${key}`, params);

    const cards: Omit<MetricCard, 'tooltip'>[] = [
      {
        key: 'receivableOpen',
        icon: 'pi pi-arrow-down-left',
        value: formatCurrencyBRL(dashboard.receivables.openAmount),
        hint: t('receivableOpen.hint', { count: dashboard.receivables.openCount }),
        trend: null,
        accent: 'primary'
      },
      {
        key: 'receivableOverdue',
        icon: 'pi pi-clock',
        value: formatCurrencyBRL(dashboard.receivables.overdueAmount),
        hint: t('receivableOverdue.hint', { count: dashboard.receivables.overdueCount }),
        trend: null,
        accent: 'warn'
      },
      {
        key: 'received',
        icon: 'pi pi-wallet',
        value: formatCurrencyBRL(dashboard.receivables.movementAmount),
        hint: t('received.hint', { count: dashboard.receivables.movementCount }),
        trend: dashboard.receivables.movementTrend ?? null,
        accent: 'success'
      },
      {
        key: 'payableOpen',
        icon: 'pi pi-arrow-up-right',
        value: formatCurrencyBRL(dashboard.payables.openAmount),
        hint: t('payableOpen.hint', { count: dashboard.payables.openCount }),
        trend: null,
        accent: 'primary'
      },
      {
        key: 'payableOverdue',
        icon: 'pi pi-clock',
        value: formatCurrencyBRL(dashboard.payables.overdueAmount),
        hint: t('payableOverdue.hint', { count: dashboard.payables.overdueCount }),
        trend: null,
        accent: 'warn'
      },
      {
        key: 'paid',
        icon: 'pi pi-wallet',
        value: formatCurrencyBRL(dashboard.payables.movementAmount),
        hint: t('paid.hint', { count: dashboard.payables.movementCount }),
        trend: dashboard.payables.movementTrend ?? null,
        accent: 'info'
      }
    ];

    return cards.map((card) => ({
      ...card,
      tooltip: this.translate.instant('financeDashboard.tooltips.metric', {
        label: this.translate.instant(`financeDashboard.metrics.${card.key}.label`),
        value: card.value,
        hint: card.hint,
        trend: this.trendDescription(card.trend)
      })
    })) as MetricCard[];
  });

  protected readonly monthlyPoints = computed<AreaChartPoint[]>(() =>
    (this.data()?.monthly ?? []).map((point) => ({
      label: this.monthLabel(point.month),
      value: point.receivedAmount,
      secondaryValue: point.paidAmount,
      tooltip: this.translate.instant('financeDashboard.charts.monthlyTooltip', {
        month: this.monthLabel(point.month),
        received: formatCurrencyBRL(point.receivedAmount),
        paid: formatCurrencyBRL(point.paidAmount),
        net: formatCurrencyBRL(point.netAmount)
      })
    }))
  );

  constructor() {
    this.load();
  }

  protected onPeriodChange(days: number | null): void {
    if (days === null) {
      return;
    }
    this.periodDays.set(days);
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);
    this.financeDashboardService.load({ from: isoDaysAgo(this.periodDays()), to: todayIso() }).subscribe({
      next: (dashboard) => {
        this.data.set(dashboard);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      }
    });
  }

  protected trendSeverity(trend: number | null): 'success' | 'danger' | 'secondary' {
    if (trend === null || trend === 0) {
      return 'secondary';
    }
    return trend > 0 ? 'success' : 'danger';
  }

  protected trendIcon(trend: number | null): string {
    if (trend === null || trend === 0) {
      return 'pi pi-minus';
    }
    return trend > 0 ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-right';
  }

  protected trendLabel(trend: number | null): string {
    if (trend === null) {
      return this.translate.instant('dashboard.trend.noBaseline');
    }
    return `${trend > 0 ? '+' : ''}${trend}%`;
  }

  protected trendDescription(trend: number | null): string {
    if (trend === null) {
      return this.translate.instant('dashboard.trend.noBaselineHint');
    }
    return this.translate.instant(trend >= 0 ? 'dashboard.trend.up' : 'dashboard.trend.down', {
      value: Math.abs(trend)
    });
  }

  private monthLabel(month: string): string {
    const [year, monthNumber] = month.split('-').map(Number);
    return `${MONTH_LABELS[monthNumber - 1]}/${String(year).slice(2)}`;
  }
}
