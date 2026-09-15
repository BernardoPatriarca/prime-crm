import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { Customer } from '../../../core/models/customer.model';
import { DomainValue } from '../../../core/models/domain-value.model';
import { Order } from '../../../core/models/order.model';
import {
  RECEIVABLE_STATUSES,
  Receivable,
  ReceivableRequest,
  ReceivableStatus
} from '../../../core/models/receivable.model';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { OrderService } from '../../../core/services/order.service';
import { ReceivableService } from '../../../core/services/receivable.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../../shared/utils/creation-route.util';
import { formatCurrencyBRL, formatIsoDate } from '../../../shared/utils/format.util';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const OPTIONS_PAGE_SIZE = 50;
const DOMAIN_OPTIONS_SIZE = 200;

const STATUS_SEVERITY: Record<ReceivableStatus, 'info' | 'success' | 'secondary'> = {
  PENDING: 'info',
  PAID: 'success',
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

function toIsoDate(value: Date | null): string | null {
  if (!value) {
    return null;
  }
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

@Component({
  selector: 'app-receivables-page',
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
    InputNumberModule,
    TextareaModule,
    SelectModule,
    DatePickerModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './receivables-page.component.html',
  styleUrl: './receivables-page.component.scss'
})
export class ReceivablesPageComponent {
  private readonly receivableService = inject(ReceivableService);
  private readonly customerService = inject(CustomerService);
  private readonly orderService = inject(OrderService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly receivables = signal<Receivable[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly statusFilter = signal<ReceivableStatus | null>(null);
  protected readonly overdueFilter = signal<boolean | null>(null);

  protected readonly customerOptions = signal<Customer[]>([]);
  protected readonly orderOptions = signal<Order[]>([]);
  protected readonly paymentMethods = signal<DomainValue[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingReceivable = signal<Receivable | null>(null);
  protected readonly saving = signal(false);

  protected readonly payDialogVisible = signal(false);
  protected readonly payingReceivable = signal<Receivable | null>(null);
  protected readonly paying = signal(false);

  protected readonly generateDialogVisible = signal(false);
  protected readonly generating = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('FINANCEIRO_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('FINANCEIRO_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('FINANCEIRO_DELETE'));

  protected readonly statusOptions = computed<SelectOption<ReceivableStatus>[]>(() => {
    this.translate.currentLang();
    return RECEIVABLE_STATUSES.map((status) => ({
      label: this.translate.instant(`receivablesPage.status.${status}`),
      value: status
    }));
  });

  protected readonly overdueOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('receivablesPage.filters.overdue'), value: true },
      { label: this.translate.instant('receivablesPage.filters.onTime'), value: false }
    ];
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    customerId: [null as string | null, [Validators.required]],
    orderId: [null as string | null],
    description: [''],
    installmentNumber: [1, [Validators.required, Validators.min(1)]],
    totalInstallments: [1, [Validators.required, Validators.min(1)]],
    dueDate: [new Date(), [Validators.required]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    paymentMethodId: [null as string | null],
    notes: ['']
  });

  protected readonly payForm = this.formBuilder.nonNullable.group({
    amount: [null as number | null],
    paymentMethodId: [null as string | null]
  });

  protected readonly generateForm = this.formBuilder.nonNullable.group({
    orderId: [null as string | null, [Validators.required]],
    installments: [2, [Validators.required, Validators.min(1), Validators.max(60)]],
    firstDueDate: [new Date(), [Validators.required]]
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

    this.load();
    this.customerService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.customerOptions.set(response.content));
    this.orderService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'orderDate,desc' })
      .subscribe((response) => this.orderOptions.set(response.content));
    this.domainValueService
      .list({ type: 'PAYMENT_METHOD', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.paymentMethods.set(response.content));
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

  protected statusSeverity(status: ReceivableStatus): 'info' | 'success' | 'secondary' {
    return STATUS_SEVERITY[status];
  }

  protected formatDate(value: string | null): string {
    return formatIsoDate(value);
  }

  protected formatAmount(value: number): string {
    return formatCurrencyBRL(value);
  }

  private load(): void {
    this.loading.set(true);
    const overdue = this.overdueFilter();
    this.receivableService
      .list({
        search: this.lastQuery.search,
        status: this.statusFilter() ?? undefined,
        overdue: overdue === null ? undefined : overdue,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.receivables.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingReceivable.set(null);
    this.form.reset({
      customerId: null,
      orderId: null,
      description: '',
      installmentNumber: 1,
      totalInstallments: 1,
      dueDate: new Date(),
      amount: 0,
      paymentMethodId: null,
      notes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(receivable: Receivable): void {
    this.editingReceivable.set(receivable);
    this.ensureCustomerOption(receivable);
    this.form.reset({
      customerId: receivable.customer?.id ?? null,
      orderId: receivable.order?.id ?? null,
      description: receivable.description ?? '',
      installmentNumber: receivable.installmentNumber,
      totalInstallments: receivable.totalInstallments,
      dueDate: new Date(receivable.dueDate),
      amount: receivable.amount,
      paymentMethodId: receivable.paymentMethod?.id ?? null,
      notes: receivable.notes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(receivable: Receivable): void {
    const customer = receivable.customer;
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
    const request: ReceivableRequest = {
      customerId: raw.customerId!,
      orderId: raw.orderId,
      description: trimmedOrNull(raw.description),
      installmentNumber: raw.installmentNumber,
      totalInstallments: raw.totalInstallments,
      dueDate: toIsoDate(raw.dueDate)!,
      amount: raw.amount,
      paymentMethodId: raw.paymentMethodId,
      notes: trimmedOrNull(raw.notes)
    };

    this.saving.set(true);
    const editing = this.editingReceivable();
    const request$ = editing
      ? this.receivableService.update(editing.id, request)
      : this.receivableService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'receivablesPage.messages.updated' : 'receivablesPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected openPayDialog(receivable: Receivable): void {
    this.payingReceivable.set(receivable);
    this.payForm.reset({ amount: null, paymentMethodId: receivable.paymentMethod?.id ?? null });
    this.payDialogVisible.set(true);
  }

  protected closePayDialog(): void {
    this.payDialogVisible.set(false);
  }

  protected confirmPay(): void {
    const receivable = this.payingReceivable();
    if (!receivable) {
      return;
    }
    const raw = this.payForm.getRawValue();

    this.paying.set(true);
    this.receivableService
      .pay(receivable.id, { amount: raw.amount, paymentMethodId: raw.paymentMethodId })
      .subscribe({
        next: () => {
          this.paying.set(false);
          this.payDialogVisible.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('receivablesPage.messages.paid')
          });
          this.load();
        },
        error: () => this.paying.set(false)
      });
  }

  protected openGenerateDialog(): void {
    this.generateForm.reset({ orderId: null, installments: 2, firstDueDate: new Date() });
    this.generateDialogVisible.set(true);
  }

  protected closeGenerateDialog(): void {
    this.generateDialogVisible.set(false);
  }

  protected confirmGenerate(): void {
    if (this.generateForm.invalid) {
      this.generateForm.markAllAsTouched();
      return;
    }
    const raw = this.generateForm.getRawValue();

    this.generating.set(true);
    this.receivableService
      .generateFromOrder(raw.orderId!, { installments: raw.installments, firstDueDate: toIsoDate(raw.firstDueDate)! })
      .subscribe({
        next: () => {
          this.generating.set(false);
          this.generateDialogVisible.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('receivablesPage.messages.generated')
          });
          this.load();
        },
        error: () => this.generating.set(false)
      });
  }

  protected confirmDelete(receivable: Receivable): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: receivable.code }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.receivableService.delete(receivable.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('receivablesPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
