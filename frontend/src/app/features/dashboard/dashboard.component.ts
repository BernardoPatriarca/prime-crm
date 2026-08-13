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
import { Dashboard, DashboardFunnelStage, DashboardRankingRow } from '../../core/models/dashboard.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { SessionStore } from '../../core/store/session.store';
import { AreaChartComponent, AreaChartPoint } from '../../shared/components/charts/area-chart.component';
import { DonutChartComponent, DonutSegment } from '../../shared/components/charts/donut-chart.component';
import { formatCompactCurrencyBRL, formatCurrencyBRL } from '../../shared/utils/format.util';

const PERIOD_OPTIONS = [7, 30, 90] as const;
const WON_COLOR = '#22C55E';
const LOST_COLOR = '#EF4444';
const OPEN_COLOR = '#1E5EFF';
const FALLBACK_STAGE_COLOR = '#8FB4FA';
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

interface FunnelBar extends DashboardFunnelStage {
  color: string;
  width: number;
  share: number;
  tooltip: string;
}

interface TaskCard {
  key: string;
  value: number;
  variant: 'danger' | 'warn' | 'success' | 'neutral';
  tooltip: string;
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
  selector: 'app-dashboard',
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
    AreaChartComponent,
    DonutChartComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly dashboardService = inject(DashboardService);
  private readonly sessionStore = inject(SessionStore);
  private readonly translate = inject(TranslateService);

  protected readonly data = signal<Dashboard | null>(null);
  protected readonly loading = signal(false);
  protected readonly failed = signal(false);
  protected readonly periodDays = signal<number>(30);

  protected readonly canSeeOpportunities = computed(() => this.sessionStore.hasPermission('OPORTUNIDADES_VIEW'));
  protected readonly canSeeTasks = computed(() => this.sessionStore.hasPermission('TAREFAS_VIEW'));

  protected readonly firstName = computed(() => {
    const name = this.sessionStore.user()?.name?.trim();
    return name ? name.split(/\s+/)[0] : '';
  });

  protected readonly periodOptions = computed<PeriodOption[]>(() => {
    this.translate.currentLang();
    return PERIOD_OPTIONS.map((days) => ({
      label: this.translate.instant('dashboard.period.days', { days }),
      value: days
    }));
  });

  protected readonly metricCards = computed<MetricCard[]>(() => {
    const metrics = this.data()?.metrics;
    if (!metrics) {
      return [];
    }
    const t = (key: string, params?: Record<string, unknown>) =>
      this.translate.instant(`dashboard.metrics.${key}`, params);

    const cards: Omit<MetricCard, 'tooltip'>[] = [
      {
        key: 'revenue',
        icon: 'pi pi-wallet',
        value: formatCurrencyBRL(metrics.wonAmount),
        hint: t('revenue.hint', { count: metrics.wonOpportunities }),
        trend: metrics.wonAmountTrend,
        accent: 'success'
      },
      {
        key: 'pipeline',
        icon: 'pi pi-chart-line',
        value: formatCurrencyBRL(metrics.openAmount),
        hint: t('pipeline.hint', { count: metrics.openOpportunities }),
        trend: null,
        accent: 'primary'
      },
      {
        key: 'winRate',
        icon: 'pi pi-percentage',
        value: `${metrics.winRate}%`,
        hint: t('winRate.hint', { won: metrics.wonOpportunities, lost: metrics.lostOpportunities }),
        trend: null,
        accent: 'info'
      },
      {
        key: 'averageTicket',
        icon: 'pi pi-tag',
        value: formatCurrencyBRL(metrics.averageTicket),
        hint: t('averageTicket.hint'),
        trend: null,
        accent: 'info'
      },
      {
        key: 'leads',
        icon: 'pi pi-bullseye',
        value: String(metrics.newLeads),
        hint: t('leads.hint', { rate: metrics.leadConversionRate, converted: metrics.convertedLeads }),
        trend: metrics.newLeadsTrend,
        accent: 'warn'
      },
      {
        key: 'customers',
        icon: 'pi pi-building',
        value: String(metrics.activeCustomers),
        hint: t('customers.hint', { count: metrics.newCustomers }),
        trend: metrics.newCustomersTrend,
        accent: 'primary'
      }
    ];

    return cards.map((card) => ({
      ...card,
      tooltip: this.translate.instant('dashboard.tooltips.metric', {
        label: this.translate.instant(`dashboard.metrics.${card.key}.label`),
        value: card.value,
        hint: card.hint,
        trend: this.trendDescription(card.trend)
      })
    }));
  });

  protected readonly monthlyPoints = computed<AreaChartPoint[]>(() =>
    (this.data()?.monthly ?? []).map((point) => ({
      label: this.monthLabel(point.month),
      value: point.wonAmount,
      secondaryValue: point.openedAmount,
      tooltip: this.translate.instant('dashboard.charts.monthlyTooltip', {
        month: this.monthLabel(point.month),
        won: formatCurrencyBRL(point.wonAmount),
        wonCount: point.wonCount,
        opened: formatCurrencyBRL(point.openedAmount)
      })
    }))
  );

  protected readonly outcomeSegments = computed<DonutSegment[]>(() => {
    const metrics = this.data()?.metrics;
    if (!metrics) {
      return [];
    }
    return [
      { label: this.translate.instant('dashboard.charts.won'), value: metrics.wonOpportunities, color: WON_COLOR },
      { label: this.translate.instant('dashboard.charts.lost'), value: metrics.lostOpportunities, color: LOST_COLOR },
      { label: this.translate.instant('dashboard.charts.open'), value: metrics.openOpportunities, color: OPEN_COLOR }
    ];
  });

  protected readonly funnelBars = computed<FunnelBar[]>(() => {
    const stages = this.data()?.funnel.stages ?? [];
    const largest = Math.max(...stages.map((stage) => stage.count), 1);
    const total = stages.reduce((sum, stage) => sum + stage.count, 0);
    return stages.map((stage) => {
      const share = total === 0 ? 0 : Math.round((stage.count / total) * 100);
      return {
        ...stage,
        color: stage.color ?? FALLBACK_STAGE_COLOR,
        width: Math.round((stage.count / largest) * 100),
        share,
        tooltip: this.translate.instant('dashboard.tooltips.funnelStage', {
          stage: stage.name,
          count: stage.count,
          amount: formatCurrencyBRL(stage.amount),
          share
        })
      };
    });
  });

  protected readonly taskCards = computed<TaskCard[]>(() => {
    const tasks = this.data()?.tasks;
    if (!tasks) {
      return [];
    }
    return [
      { key: 'overdue', value: tasks.overdue, variant: 'danger' },
      { key: 'dueToday', value: tasks.dueToday, variant: 'warn' },
      { key: 'pending', value: tasks.pending, variant: 'neutral' },
      { key: 'inProgress', value: tasks.inProgress, variant: 'neutral' },
      { key: 'completedThisWeek', value: tasks.completedThisWeek, variant: 'success' }
    ].map((card) => ({
      ...card,
      tooltip: this.translate.instant(`dashboard.tooltips.tasks.${card.key}`, { count: card.value })
    })) as TaskCard[];
  });

  protected readonly funnelTotalAmount = computed(() =>
    (this.data()?.funnel.stages ?? []).reduce((sum, stage) => sum + stage.amount, 0)
  );

  protected readonly hasTasks = computed(() => {
    const tasks = this.data()?.tasks;
    return !!tasks && tasks.pending + tasks.inProgress + tasks.overdue + tasks.completedThisWeek > 0;
  });

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
    this.dashboardService.load({ from: isoDaysAgo(this.periodDays()), to: todayIso() }).subscribe({
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

  protected formatAmount(value: number): string {
    return formatCurrencyBRL(value);
  }

  protected formatCompact(value: number): string {
    return formatCompactCurrencyBRL(value);
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

  protected rankingTooltip(row: DashboardRankingRow): string {
    return this.translate.instant('dashboard.tooltips.ranking', {
      owner: row.owner,
      amount: formatCurrencyBRL(row.amount),
      count: row.count,
      share: row.share
    });
  }

  private monthLabel(month: string): string {
    const [year, monthNumber] = month.split('-').map(Number);
    return `${MONTH_LABELS[monthNumber - 1]}/${String(year).slice(2)}`;
  }
}
