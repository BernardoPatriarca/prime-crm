import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { ProgressBarModule } from 'primeng/progressbar';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { AdminUser } from '../../core/models/admin-user.model';
import { SalesGoal, SalesGoalRequest } from '../../core/models/sales-goal.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { SalesGoalService } from '../../core/services/sales-goal.service';
import { SessionStore } from '../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../shared/utils/creation-route.util';
import { formatCurrencyBRL } from '../../shared/utils/format.util';

const OPTIONS_PAGE_SIZE = 50;

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

function trimmedOrNull(value: string | null): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length > 0 ? trimmed : null;
}

function toIsoMonth(value: Date | null): string | null {
  if (!value) {
    return null;
  }
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}-01`;
}

@Component({
  selector: 'app-sales-goals-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    TextareaModule,
    SelectModule,
    DatePickerModule,
    ProgressBarModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './sales-goals-page.component.html',
  styleUrl: './sales-goals-page.component.scss'
})
export class SalesGoalsPageComponent {
  private readonly salesGoalService = inject(SalesGoalService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly goals = signal<SalesGoal[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly userOptions = signal<AdminUser[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingGoal = signal<SalesGoal | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('METAS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('METAS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('METAS_DELETE'));

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    ownerUserId: [null as string | null, [Validators.required]],
    referenceMonth: [new Date(), [Validators.required]],
    targetAmount: [0, [Validators.required, Validators.min(0.01)]],
    notes: ['']
  });

  constructor() {
    openCreateDialogFromRoute(() => {
      if (this.canCreate()) {
        this.openCreateDialog();
      }
    });

    this.load();
    this.adminUserService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.userOptions.set(response.content));
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  protected formatAmount(value: number): string {
    return formatCurrencyBRL(value);
  }

  protected formatMonth(value: string): string {
    const [year, month] = value.split('-');
    return `${month}/${year}`;
  }

  protected achievementSeverity(percent: number): 'danger' | 'warn' | 'success' {
    if (percent >= 100) {
      return 'success';
    }
    if (percent >= 50) {
      return 'warn';
    }
    return 'danger';
  }

  private load(): void {
    this.loading.set(true);
    this.salesGoalService
      .list({
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.goals.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingGoal.set(null);
    this.form.reset({
      ownerUserId: null,
      referenceMonth: new Date(),
      targetAmount: 0,
      notes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(goal: SalesGoal): void {
    this.editingGoal.set(goal);
    this.ensureOwnerOption(goal);
    const [year, month] = goal.referenceMonth.split('-').map(Number);
    this.form.reset({
      ownerUserId: goal.owner?.id ?? null,
      referenceMonth: new Date(year, month - 1, 1),
      targetAmount: goal.targetAmount,
      notes: goal.notes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureOwnerOption(goal: SalesGoal): void {
    const owner = goal.owner;
    if (!owner || this.userOptions().some((option) => option.id === owner.id)) {
      return;
    }
    this.adminUserService.getById(owner.id).subscribe((loaded) => {
      this.userOptions.set([loaded, ...this.userOptions()]);
    });
  }

  protected closeDialog(): void {
    this.dialogVisible.set(false);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: SalesGoalRequest = {
      ownerUserId: raw.ownerUserId!,
      referenceMonth: toIsoMonth(raw.referenceMonth)!,
      targetAmount: raw.targetAmount,
      notes: trimmedOrNull(raw.notes)
    };

    this.saving.set(true);
    const editing = this.editingGoal();
    const request$ = editing
      ? this.salesGoalService.update(editing.id, request)
      : this.salesGoalService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'salesGoalsPage.messages.updated' : 'salesGoalsPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(goal: SalesGoal): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', {
        name: `${goal.owner?.name ?? ''} - ${this.formatMonth(goal.referenceMonth)}`
      }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.salesGoalService.delete(goal.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('salesGoalsPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
