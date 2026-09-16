import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, debounceTime, switchMap } from 'rxjs';
import {
  CALENDAR_EVENT_STATUSES,
  CalendarEvent,
  CalendarEventRequest,
  CalendarEventStatus
} from '../../core/models/calendar-event.model';
import { AdminUser } from '../../core/models/admin-user.model';
import { Customer } from '../../core/models/customer.model';
import { DomainValue } from '../../core/models/domain-value.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { CalendarEventService } from '../../core/services/calendar-event.service';
import { CustomerService } from '../../core/services/customer.service';
import { DomainValueService } from '../../core/services/domain-value.service';
import { SessionStore } from '../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../shared/utils/creation-route.util';
import { formatInstant } from '../../shared/utils/format.util';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const OPTIONS_PAGE_SIZE = 50;
const DOMAIN_OPTIONS_SIZE = 200;
const CALENDAR_GRID_SIZE = 42;

const STATUS_SEVERITY: Record<CalendarEventStatus, 'info' | 'success' | 'secondary'> = {
  SCHEDULED: 'info',
  DONE: 'success',
  CANCELED: 'secondary'
};

interface SelectOption<T> {
  label: string;
  value: T;
}

interface CalendarDay {
  date: Date;
  inCurrentMonth: boolean;
  isToday: boolean;
  events: CalendarEvent[];
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

function startOfDay(date: Date): Date {
  const copy = new Date(date);
  copy.setHours(0, 0, 0, 0);
  return copy;
}

function isSameDay(a: Date, b: Date): boolean {
  return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

@Component({
  selector: 'app-agenda-page',
  standalone: true,
  imports: [
    DatePipe,
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
    SelectButtonModule,
    DatePickerModule,
    CheckboxModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './agenda-page.component.html',
  styleUrl: './agenda-page.component.scss'
})
export class AgendaPageComponent {
  private readonly calendarEventService = inject(CalendarEventService);
  private readonly customerService = inject(CustomerService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly viewMode = signal<'calendar' | 'list'>('calendar');

  protected readonly events = signal<CalendarEvent[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly currentMonth = signal(startOfDay(new Date(new Date().getFullYear(), new Date().getMonth(), 1)));
  protected readonly monthEvents = signal<CalendarEvent[]>([]);
  protected readonly monthLoading = signal(false);

  protected readonly statusFilter = signal<CalendarEventStatus | null>(null);
  protected readonly assigneeFilter = signal<string | null>(null);
  protected readonly overdueFilter = signal<boolean | null>(null);

  protected readonly customerOptions = signal<Customer[]>([]);
  protected readonly userOptions = signal<AdminUser[]>([]);
  protected readonly types = signal<DomainValue[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingEvent = signal<CalendarEvent | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('AGENDA_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('AGENDA_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('AGENDA_DELETE'));

  protected readonly viewOptions = computed<SelectOption<'calendar' | 'list'>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('agendaPage.views.calendar'), value: 'calendar' },
      { label: this.translate.instant('agendaPage.views.list'), value: 'list' }
    ];
  });

  protected readonly statusOptions = computed<SelectOption<CalendarEventStatus>[]>(() => {
    this.translate.currentLang();
    return CALENDAR_EVENT_STATUSES.map((status) => ({
      label: this.translate.instant(`agendaPage.status.${status}`),
      value: status
    }));
  });

  protected readonly overdueOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('agendaPage.filters.overdue'), value: true },
      { label: this.translate.instant('agendaPage.filters.onTime'), value: false }
    ];
  });

  protected readonly monthLabel = computed(() =>
    this.currentMonth().toLocaleDateString(this.translate.currentLang() || 'pt-BR', {
      month: 'long',
      year: 'numeric'
    })
  );

  protected readonly calendarDays = computed<CalendarDay[]>(() => {
    const month = this.currentMonth();
    const today = startOfDay(new Date());
    const firstWeekday = month.getDay();
    const gridStart = new Date(month);
    gridStart.setDate(gridStart.getDate() - firstWeekday);

    const events = this.monthEvents();
    const days: CalendarDay[] = [];
    for (let i = 0; i < CALENDAR_GRID_SIZE; i++) {
      const date = new Date(gridStart);
      date.setDate(date.getDate() + i);
      days.push({
        date,
        inCurrentMonth: date.getMonth() === month.getMonth(),
        isToday: isSameDay(date, today),
        events: events.filter((event) => isSameDay(new Date(event.startAt), date))
      });
    }
    return days;
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    typeId: [null as string | null],
    status: ['SCHEDULED' as CalendarEventStatus, [Validators.required]],
    startAt: [null as Date | null, [Validators.required]],
    endAt: [null as Date | null],
    allDay: [false],
    location: [''],
    reminderAt: [null as Date | null],
    assignedUserId: [null as string | null],
    customerId: [null as string | null]
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

    openCreateDialogFromRoute(() => {
      if (this.canCreate()) {
        this.openCreateDialog();
      }
    });

    this.loadMonth();
    this.customerService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.customerOptions.set(response.content));
    this.adminUserService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.userOptions.set(response.content));
    this.domainValueService
      .list({ type: 'TASK_TYPE', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.types.set(response.content));
  }

  protected onViewModeChange(mode: 'calendar' | 'list'): void {
    this.viewMode.set(mode);
    if (mode === 'list') {
      this.load();
    } else {
      this.loadMonth();
    }
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

  protected statusSeverity(status: CalendarEventStatus): 'info' | 'success' | 'secondary' {
    return STATUS_SEVERITY[status];
  }

  protected formatDateTime(value: string | null): string {
    return formatInstant(value);
  }

  protected previousMonth(): void {
    const month = this.currentMonth();
    this.currentMonth.set(startOfDay(new Date(month.getFullYear(), month.getMonth() - 1, 1)));
    this.loadMonth();
  }

  protected nextMonth(): void {
    const month = this.currentMonth();
    this.currentMonth.set(startOfDay(new Date(month.getFullYear(), month.getMonth() + 1, 1)));
    this.loadMonth();
  }

  protected goToToday(): void {
    const today = new Date();
    this.currentMonth.set(startOfDay(new Date(today.getFullYear(), today.getMonth(), 1)));
    this.loadMonth();
  }

  private loadMonth(): void {
    const month = this.currentMonth();
    const firstWeekday = month.getDay();
    const gridStart = new Date(month);
    gridStart.setDate(gridStart.getDate() - firstWeekday);
    const gridEnd = new Date(gridStart);
    gridEnd.setDate(gridEnd.getDate() + CALENDAR_GRID_SIZE);

    this.monthLoading.set(true);
    this.calendarEventService
      .range({ from: gridStart.toISOString(), to: gridEnd.toISOString() })
      .subscribe({
        next: (events) => {
          this.monthEvents.set(events);
          this.monthLoading.set(false);
        },
        error: () => this.monthLoading.set(false)
      });
  }

  private load(): void {
    this.loading.set(true);
    const overdue = this.overdueFilter();
    this.calendarEventService
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
          this.events.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(day?: Date): void {
    this.editingEvent.set(null);
    const startAt = day ? new Date(day.getFullYear(), day.getMonth(), day.getDate(), 9, 0) : null;
    this.form.reset({
      title: '',
      description: '',
      typeId: null,
      status: 'SCHEDULED',
      startAt,
      endAt: null,
      allDay: false,
      location: '',
      reminderAt: null,
      assignedUserId: null,
      customerId: null
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(event: CalendarEvent): void {
    this.editingEvent.set(event);
    this.ensureCustomerOption(event);
    this.form.reset({
      title: event.title,
      description: event.description ?? '',
      typeId: event.type?.id ?? null,
      status: event.status,
      startAt: new Date(event.startAt),
      endAt: event.endAt ? new Date(event.endAt) : null,
      allDay: event.allDay,
      location: event.location ?? '',
      reminderAt: event.reminderAt ? new Date(event.reminderAt) : null,
      assignedUserId: event.assignee?.id ?? null,
      customerId: event.customer?.id ?? null
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(event: CalendarEvent): void {
    const customer = event.customer;
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
    const request: CalendarEventRequest = {
      title: raw.title.trim(),
      description: trimmedOrNull(raw.description),
      typeId: raw.typeId,
      status: raw.status,
      startAt: raw.startAt!.toISOString(),
      endAt: raw.endAt ? raw.endAt.toISOString() : null,
      allDay: raw.allDay,
      location: trimmedOrNull(raw.location),
      reminderAt: raw.reminderAt ? raw.reminderAt.toISOString() : null,
      assignedUserId: raw.assignedUserId,
      customerId: raw.customerId
    };

    this.saving.set(true);
    const editing = this.editingEvent();
    const request$ = editing
      ? this.calendarEventService.update(editing.id, request)
      : this.calendarEventService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'agendaPage.messages.updated' : 'agendaPage.messages.created')
        });
        this.refresh();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(event: CalendarEvent): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: event.title }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.calendarEventService.delete(event.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('agendaPage.messages.deleted')
          });
          this.refresh();
        });
      }
    });
  }

  private refresh(): void {
    if (this.viewMode() === 'list') {
      this.load();
    } else {
      this.loadMonth();
    }
  }
}
