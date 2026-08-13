import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { AdminUser } from '../../core/models/admin-user.model';
import { REPORT_GROUP_BY_OPTIONS, Report, ReportGroupBy, ReportKey, ReportQuery } from '../../core/models/report.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { ReportService } from '../../core/services/report.service';
import { SessionStore } from '../../core/store/session.store';
import { downloadBlob } from '../../shared/utils/file-download.util';
import { formatCurrencyBRL } from '../../shared/utils/format.util';

const USER_OPTIONS_SIZE = 50;

interface GroupByOption {
  label: string;
  value: ReportGroupBy;
}

@Component({
  selector: 'app-reports-page',
  standalone: true,
  imports: [
    FormsModule,
    TranslatePipe,
    ButtonModule,
    SelectModule,
    DatePickerModule,
    TableModule,
    SkeletonModule
  ],
  templateUrl: './reports-page.component.html',
  styleUrl: './reports-page.component.scss'
})
export class ReportsPageComponent {
  private readonly reportService = inject(ReportService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly translate = inject(TranslateService);

  readonly report = input.required<ReportKey>();

  protected readonly groupBy = signal<ReportGroupBy | null>(null);
  protected readonly from = signal<Date | null>(null);
  protected readonly to = signal<Date | null>(null);
  protected readonly userId = signal<string | null>(null);

  protected readonly result = signal<Report | null>(null);
  protected readonly loading = signal(false);
  protected readonly exporting = signal(false);
  protected readonly userOptions = signal<AdminUser[]>([]);

  protected readonly canExport = computed(() => this.sessionStore.hasPermission('RELATORIOS_EXPORT'));

  protected readonly groupByOptions = computed<GroupByOption[]>(() => {
    this.translate.currentLang();
    return REPORT_GROUP_BY_OPTIONS[this.report()].map((option) => ({
      label: this.translate.instant(`reportsPage.groupings.${option}`),
      value: option
    }));
  });

  protected readonly userLabel = computed(() => {
    this.translate.currentLang();
    return this.translate.instant(
      this.report() === 'tasks' ? 'reportsPage.filters.assignee' : 'reportsPage.filters.owner'
    );
  });

  constructor() {
    effect(() => {
      const options = REPORT_GROUP_BY_OPTIONS[this.report()];
      untracked(() => {
        this.groupBy.set(options[0]);
        this.result.set(null);
        this.load();
      });
    });

    this.adminUserService
      .list({ size: USER_OPTIONS_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.userOptions.set(response.content));
  }

  protected onFilterChange(): void {
    this.load();
  }

  protected clearFilters(): void {
    this.from.set(null);
    this.to.set(null);
    this.userId.set(null);
    this.load();
  }

  protected formatAmount(value: number | null): string {
    return formatCurrencyBRL(value);
  }

  protected export(): void {
    const query = this.currentQuery();
    if (!query) {
      return;
    }
    this.exporting.set(true);
    this.reportService.export(this.report(), query).subscribe({
      next: (blob) => {
        downloadBlob(blob, `${this.report()}-${query.groupBy.toLowerCase()}.csv`);
        this.exporting.set(false);
      },
      error: () => this.exporting.set(false)
    });
  }

  private load(): void {
    const query = this.currentQuery();
    if (!query) {
      return;
    }
    this.loading.set(true);
    this.reportService.load(this.report(), query).subscribe({
      next: (report) => {
        this.result.set(report);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  private currentQuery(): ReportQuery | null {
    const groupBy = this.groupBy();
    if (!groupBy) {
      return null;
    }
    return {
      groupBy,
      from: this.from()?.toISOString() ?? null,
      to: this.to()?.toISOString() ?? null,
      userId: this.userId()
    };
  }
}
