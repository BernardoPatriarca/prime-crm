import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { ProductivityDashboard, ProductivityRankingRow } from '../../../core/models/productivity-dashboard.model';
import { ProductivityDashboardService } from '../../../core/services/productivity-dashboard.service';

const PERIOD_OPTIONS = [7, 30, 90] as const;

interface PeriodOption {
  label: string;
  value: number;
}

interface TaskCard {
  key: string;
  value: number;
  variant: 'danger' | 'warn' | 'success' | 'neutral';
  tooltip: string;
}

interface AgendaCard {
  key: string;
  value: number;
  variant: 'danger' | 'success' | 'neutral';
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
  selector: 'app-productivity-dashboard',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    TranslatePipe,
    CardModule,
    ButtonModule,
    SelectButtonModule,
    SkeletonModule,
    TooltipModule
  ],
  templateUrl: './productivity-dashboard.component.html',
  styleUrl: './productivity-dashboard.component.scss'
})
export class ProductivityDashboardComponent {
  private readonly productivityDashboardService = inject(ProductivityDashboardService);
  private readonly translate = inject(TranslateService);

  protected readonly data = signal<ProductivityDashboard | null>(null);
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
      tooltip: this.translate.instant(`productivityDashboard.tooltips.tasks.${card.key}`, { count: card.value })
    })) as TaskCard[];
  });

  protected readonly agendaCards = computed<AgendaCard[]>(() => {
    const agenda = this.data()?.agenda;
    if (!agenda) {
      return [];
    }
    return [
      { key: 'scheduledToday', value: agenda.scheduledToday, variant: 'neutral' },
      { key: 'scheduledThisWeek', value: agenda.scheduledThisWeek, variant: 'neutral' },
      { key: 'overdue', value: agenda.overdue, variant: 'danger' }
    ];
  });

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
    this.productivityDashboardService.load({ from: isoDaysAgo(this.periodDays()), to: todayIso() }).subscribe({
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

  protected rankingTooltip(row: ProductivityRankingRow): string {
    return this.translate.instant('productivityDashboard.tooltips.ranking', {
      owner: row.owner,
      count: row.completedCount,
      share: row.share
    });
  }
}
