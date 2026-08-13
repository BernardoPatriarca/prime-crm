import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AUDIT_ACTIONS, AuditAction, AuditLog } from '../../../core/models/audit-log.model';
import { AdminUser } from '../../../core/models/admin-user.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AuditLogService } from '../../../core/services/audit-log.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { downloadBlob } from '../../../shared/utils/file-download.util';
import { formatInstant } from '../../../shared/utils/format.util';

const USER_OPTIONS_SIZE = 50;

const ACTION_SEVERITY: Record<AuditAction, 'success' | 'info' | 'danger' | 'warn' | 'secondary'> = {
  CREATE: 'success',
  UPDATE: 'info',
  DELETE: 'danger',
  LOGIN: 'secondary',
  LOGIN_FAILED: 'danger',
  LOGOUT: 'secondary',
  EXPORT: 'warn'
};

interface ActionOption {
  label: string;
  value: AuditAction;
}

interface ChangeEntry {
  field: string;
  oldValue: string;
  newValue: string;
  hasPrevious: boolean;
}

function isDiff(value: unknown): value is { old: unknown; new: unknown } {
  return typeof value === 'object' && value !== null && 'old' in value && 'new' in value;
}

function display(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return '-';
  }
  return typeof value === 'object' ? JSON.stringify(value) : String(value);
}

@Component({
  selector: 'app-audit-page',
  standalone: true,
  imports: [
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    SelectModule,
    DatePickerModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './audit-page.component.html',
  styleUrl: './audit-page.component.scss'
})
export class AuditPageComponent {
  private readonly auditLogService = inject(AuditLogService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly translate = inject(TranslateService);

  protected readonly entries = signal<AuditLog[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly exporting = signal(false);

  protected readonly entityFilter = signal<string | null>(null);
  protected readonly actionFilter = signal<AuditAction | null>(null);
  protected readonly userFilter = signal<string | null>(null);
  protected readonly from = signal<Date | null>(null);
  protected readonly to = signal<Date | null>(null);

  protected readonly entityNames = signal<string[]>([]);
  protected readonly userOptions = signal<AdminUser[]>([]);

  protected readonly selectedEntry = signal<AuditLog | null>(null);

  protected readonly canExport = computed(() => this.sessionStore.hasPermission('AUDITORIA_EXPORT'));

  protected readonly actionOptions = computed<ActionOption[]>(() => {
    this.translate.currentLang();
    return AUDIT_ACTIONS.map((action) => ({
      label: this.translate.instant(`auditPage.actions.${action}`),
      value: action
    }));
  });

  protected readonly changeEntries = computed<ChangeEntry[]>(() => {
    const changes = this.selectedEntry()?.changes;
    if (!changes) {
      return [];
    }
    return Object.entries(changes).map(([field, value]) => ({
      field,
      oldValue: isDiff(value) ? display(value.old) : '',
      newValue: isDiff(value) ? display(value.new) : display(value),
      hasPrevious: isDiff(value)
    }));
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };

  constructor() {
    this.load();
    this.auditLogService.entityNames().subscribe((names) => this.entityNames.set(names));
    this.adminUserService
      .list({ size: USER_OPTIONS_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.userOptions.set(response.content));
  }

  protected onFilterChange(): void {
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  protected clearFilters(): void {
    this.entityFilter.set(null);
    this.actionFilter.set(null);
    this.userFilter.set(null);
    this.from.set(null);
    this.to.set(null);
    this.onFilterChange();
  }

  protected actionSeverity(action: AuditAction): string {
    return ACTION_SEVERITY[action];
  }

  protected formatDateTime(value: string | null): string {
    return formatInstant(value);
  }

  protected openDetails(entry: AuditLog): void {
    this.selectedEntry.set(entry);
  }

  protected closeDetails(): void {
    this.selectedEntry.set(null);
  }

  protected export(): void {
    this.exporting.set(true);
    this.auditLogService.export(this.currentQuery()).subscribe({
      next: (blob) => {
        downloadBlob(blob, 'auditoria.csv');
        this.exporting.set(false);
      },
      error: () => this.exporting.set(false)
    });
  }

  private load(): void {
    this.loading.set(true);
    this.auditLogService
      .list({ ...this.currentQuery(), page: this.lastQuery.page, size: this.lastQuery.size })
      .subscribe({
        next: (response) => {
          this.entries.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  private currentQuery() {
    return {
      search: this.lastQuery.search,
      entityName: this.entityFilter() ?? undefined,
      action: this.actionFilter() ?? undefined,
      userId: this.userFilter() ?? undefined,
      from: this.from()?.toISOString(),
      to: this.to()?.toISOString()
    };
  }
}
