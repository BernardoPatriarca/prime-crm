import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { CommercialDashboard } from '../../../core/models/commercial-dashboard.model';
import { CommercialDashboardService } from '../../../core/services/commercial-dashboard.service';
import { AreaChartComponent, AreaChartPoint } from '../../../shared/components/charts/area-chart.component';
import { formatCurrencyBRL } from '../../../shared/utils/format.util';

const PERIOD_OPTIONS = [30, 90, 180] as const;
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
  selector: 'app-commercial-dashboard',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    TranslatePipe,
    CardModule,
    ButtonModule,
    SelectButtonModule,
    SkeletonModule,
    TooltipModule,
    AreaChartComponent
  ],
  templateUrl: './commercial-dashboard.component.html',
  styleUrl: './commercial-dashboard.component.scss'
})
export class CommercialDashboardComponent {
  private readonly commercialDashboardService = inject(CommercialDashboardService);
  private readonly translate = inject(TranslateService);

  protected readonly data = signal<CommercialDashboard | null>(null);
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
      this.translate.instant(`commercialDashboard.metrics.${key}`, params);

    const cards: Omit<MetricCard, 'tooltip'>[] = [
      {
        key: 'proposalsTotal',
        icon: 'pi pi-file',
        value: formatCurrencyBRL(dashboard.proposals.totalAmount),
        hint: t('proposalsTotal.hint', { count: dashboard.proposals.totalCount }),
        accent: 'primary'
      },
      {
        key: 'proposalsConversion',
        icon: 'pi pi-percentage',
        value: `${dashboard.proposals.conversionRate}%`,
        hint: t('proposalsConversion.hint', {
          closed: dashboard.proposals.closedCount,
          total: dashboard.proposals.totalCount
        }),
        accent: 'success'
      },
      {
        key: 'ordersTotal',
        icon: 'pi pi-truck',
        value: formatCurrencyBRL(dashboard.orders.totalAmount),
        hint: t('ordersTotal.hint', { count: dashboard.orders.totalCount }),
        accent: 'primary'
      },
      {
        key: 'ordersConversion',
        icon: 'pi pi-percentage',
        value: `${dashboard.orders.conversionRate}%`,
        hint: t('ordersConversion.hint', {
          closed: dashboard.orders.closedCount,
          total: dashboard.orders.totalCount
        }),
        accent: 'success'
      },
      {
        key: 'contractsActive',
        icon: 'pi pi-verified',
        value: formatCurrencyBRL(dashboard.contracts.activeRecurringAmount),
        hint: t('contractsActive.hint', { count: dashboard.contracts.activeCount }),
        accent: 'info'
      },
      {
        key: 'contractsExpiring',
        icon: 'pi pi-clock',
        value: formatCurrencyBRL(dashboard.contracts.expiringAmount),
        hint: t('contractsExpiring.hint', { count: dashboard.contracts.expiringCount }),
        accent: 'warn'
      }
    ];

    return cards.map((card) => ({
      ...card,
      tooltip: this.translate.instant('commercialDashboard.tooltips.metric', {
        label: this.translate.instant(`commercialDashboard.metrics.${card.key}.label`),
        value: card.value,
        hint: card.hint
      })
    }));
  });

  protected readonly monthlyPoints = computed<AreaChartPoint[]>(() =>
    (this.data()?.monthly ?? []).map((point) => ({
      label: this.monthLabel(point.month),
      value: point.proposalsAmount,
      secondaryValue: point.ordersAmount,
      tooltip: this.translate.instant('commercialDashboard.charts.monthlyTooltip', {
        month: this.monthLabel(point.month),
        proposals: formatCurrencyBRL(point.proposalsAmount),
        orders: formatCurrencyBRL(point.ordersAmount)
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
    this.commercialDashboardService.load({ from: isoDaysAgo(this.periodDays()), to: todayIso() }).subscribe({
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

  private monthLabel(month: string): string {
    const [year, monthNumber] = month.split('-').map(Number);
    return `${MONTH_LABELS[monthNumber - 1]}/${String(year).slice(2)}`;
  }
}
