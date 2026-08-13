import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AdminUser } from '../../core/models/admin-user.model';
import { Customer } from '../../core/models/customer.model';
import { DomainValue } from '../../core/models/domain-value.model';
import { TASK_STATUSES, Task, TaskRequest, TaskStatus } from '../../core/models/task.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { CustomerService } from '../../core/services/customer.service';
import { DomainValueService } from '../../core/services/domain-value.service';
import { TaskService } from '../../core/services/task.service';
import { SessionStore } from '../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../shared/components/generic-table/generic-table.component';
import { formatInstant } from '../../shared/utils/format.util';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const OPTIONS_PAGE_SIZE = 50;
const DOMAIN_OPTIONS_SIZE = 200;

const STATUS_SEVERITY: Record<TaskStatus, 'info' | 'warn' | 'success' | 'secondary'> = {
  PENDING: 'info',
  IN_PROGRESS: 'warn',
  DONE: 'success',
  CANCELED: 'secondary'
};

interface SelectOption<T> {
  label: string;
  value: T;
}

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

@Component({
  selector: 'app-tasks-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    DatePickerModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './tasks-page.component.html',
  styleUrl: './tasks-page.component.scss'
})
export class TasksPageComponent {
  private readonly taskService = inject(TaskService);
  private readonly customerService = inject(CustomerService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly tasks = signal<Task[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly statusFilter = signal<TaskStatus | null>(null);
  protected readonly assigneeFilter = signal<string | null>(null);
  protected readonly overdueFilter = signal<boolean | null>(null);

  protected readonly customerOptions = signal<Customer[]>([]);
  protected readonly userOptions = signal<AdminUser[]>([]);
  protected readonly types = signal<DomainValue[]>([]);
  protected readonly priorities = signal<DomainValue[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingTask = signal<Task | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('TAREFAS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('TAREFAS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('TAREFAS_DELETE'));

  protected readonly statusOptions = computed<SelectOption<TaskStatus>[]>(() => {
    this.translate.currentLang();
    return TASK_STATUSES.map((status) => ({
      label: this.translate.instant(`tasksPage.status.${status}`),
      value: status
    }));
  });

  protected readonly overdueOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('tasksPage.filters.overdue'), value: true },
      { label: this.translate.instant('tasksPage.filters.onTime'), value: false }
    ];
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    typeId: [null as string | null],
    priorityId: [null as string | null],
    status: ['PENDING' as TaskStatus, [Validators.required]],
    dueAt: [null as Date | null],
    reminderAt: [null as Date | null],
    assignedUserId: [null as string | null],
    customerId: [null as string | null],
    resultNotes: ['']
  });

  constructor() {
    this.customerSearch
      .pipe(
        debounceTime(CUSTOMER_SEARCH_DEBOUNCE_MS),
        switchMap((search) =>
          this.customerService.list({ search: search || undefined, size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
        ),
        takeUntilDestroyed()
      )
      .subscribe((response) => this.customerOptions.set(response.content));

    this.load();
    this.customerService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.customerOptions.set(response.content));
    this.adminUserService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.userOptions.set(response.content));
    this.domainValueService
      .list({ type: 'TASK_TYPE', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.types.set(response.content));
    this.domainValueService
      .list({ type: 'PRIORITY', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.priorities.set(response.content));
  }

  protected onCustomerFilter(search: string): void {
    this.customerSearch.next(search);
  }

  protected onFilterChange(): void {
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  protected statusSeverity(status: TaskStatus): string {
    return STATUS_SEVERITY[status];
  }

  protected formatDateTime(value: string | null): string {
    return formatInstant(value);
  }

  private load(): void {
    this.loading.set(true);
    const overdue = this.overdueFilter();
    this.taskService
      .list({
        search: this.lastQuery.search,
        status: this.statusFilter() ?? undefined,
        assignedUserId: this.assigneeFilter() ?? undefined,
        overdue: overdue === null ? undefined : overdue,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.tasks.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingTask.set(null);
    this.form.reset({
      title: '',
      description: '',
      typeId: null,
      priorityId: null,
      status: 'PENDING',
      dueAt: null,
      reminderAt: null,
      assignedUserId: null,
      customerId: null,
      resultNotes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(task: Task): void {
    this.editingTask.set(task);
    this.ensureCustomerOption(task);
    this.form.reset({
      title: task.title,
      description: task.description ?? '',
      typeId: task.type?.id ?? null,
      priorityId: task.priority?.id ?? null,
      status: task.status,
      dueAt: task.dueAt ? new Date(task.dueAt) : null,
      reminderAt: task.reminderAt ? new Date(task.reminderAt) : null,
      assignedUserId: task.assignee?.id ?? null,
      customerId: task.customer?.id ?? null,
      resultNotes: task.resultNotes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(task: Task): void {
    const customer = task.customer;
    if (!customer || this.customerOptions().some((option) => option.id === customer.id)) {
      return;
    }
    this.customerService.getById(customer.id).subscribe((loaded) => {
      this.customerOptions.set([loaded, ...this.customerOptions()]);
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
    const request: TaskRequest = {
      title: raw.title.trim(),
      description: trimmedOrNull(raw.description),
      typeId: raw.typeId,
      priorityId: raw.priorityId,
      status: raw.status,
      dueAt: raw.dueAt ? raw.dueAt.toISOString() : null,
      reminderAt: raw.reminderAt ? raw.reminderAt.toISOString() : null,
      assignedUserId: raw.assignedUserId,
      customerId: raw.customerId,
      resultNotes: trimmedOrNull(raw.resultNotes)
    };

    this.saving.set(true);
    const editing = this.editingTask();
    const request$ = editing ? this.taskService.update(editing.id, request) : this.taskService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'tasksPage.messages.updated' : 'tasksPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected complete(task: Task): void {
    this.taskService.changeStatus(task.id, { status: 'DONE' }).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('tasksPage.messages.completed')
      });
      this.load();
    });
  }

  protected confirmDelete(task: Task): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: task.title }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.taskService.delete(task.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('tasksPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
