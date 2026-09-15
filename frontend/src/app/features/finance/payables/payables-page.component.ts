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
import { PAYABLE_STATUSES, Payable, PayableRequest, PayableStatus } from '../../../core/models/payable.model';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { PayableService } from '../../../core/services/payable.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../../shared/utils/creation-route.util';
import { formatCurrencyBRL, formatIsoDate } from '../../../shared/utils/format.util';

const SUPPLIER_SEARCH_DEBOUNCE_MS = 300;
const OPTIONS_PAGE_SIZE = 50;
const DOMAIN_OPTIONS_SIZE = 200;

const STATUS_SEVERITY: Record<PayableStatus, 'info' | 'success' | 'secondary'> = {
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
  selector: 'app-payables-page',
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
  templateUrl: './payables-page.component.html',
  styleUrl: './payables-page.component.scss'
})
export class PayablesPageComponent {
  private readonly payableService = inject(PayableService);
  private readonly customerService = inject(CustomerService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly payables = signal<Payable[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly statusFilter = signal<PayableStatus | null>(null);
  protected readonly overdueFilter = signal<boolean | null>(null);

  protected readonly supplierOptions = signal<Customer[]>([]);
  protected readonly categories = signal<DomainValue[]>([]);
  protected readonly paymentMethods = signal<DomainValue[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingPayable = signal<Payable | null>(null);
  protected readonly saving = signal(false);

  protected readonly payDialogVisible = signal(false);
  protected readonly payingPayable = signal<Payable | null>(null);
  protected readonly paying = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('FINANCEIRO_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('FINANCEIRO_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('FINANCEIRO_DELETE'));

  protected readonly statusOptions = computed<SelectOption<PayableStatus>[]>(() => {
    this.translate.currentLang();
    return PAYABLE_STATUSES.map((status) => ({
      label: this.translate.instant(`payablesPage.status.${status}`),
      value: status
    }));
  });

  protected readonly overdueOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('payablesPage.filters.overdue'), value: true },
      { label: this.translate.instant('payablesPage.filters.onTime'), value: false }
    ];
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly supplierSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    supplierId: [null as string | null, [Validators.required]],
    categoryId: [null as string | null],
    description: ['', [Validators.required]],
    dueDate: [new Date(), [Validators.required]],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    paymentMethodId: [null as string | null],
    notes: ['']
  });

  protected readonly payForm = this.formBuilder.nonNullable.group({
    amount: [null as number | null],
    paymentMethodId: [null as string | null]
  });

  constructor() {
    this.supplierSearch
      .pipe(
        debounceTime(SUPPLIER_SEARCH_DEBOUNCE_MS),
        switchMap((search) =>
          this.customerService.list({ search: search || undefined, size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
        ),
        takeUntilDestroyed()
      )
      .subscribe((response) => this.supplierOptions.set(response.content));

    openCreateDialogFromRoute(() => {
      if (this.canCreate()) {
        this.openCreateDialog();
      }
    });

    this.load();
    this.customerService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.supplierOptions.set(response.content));
    this.domainValueService
      .list({ type: 'CATEGORY', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.categories.set(response.content));
    this.domainValueService
      .list({ type: 'PAYMENT_METHOD', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.paymentMethods.set(response.content));
  }

  protected onSupplierFilter(search: string): void {
    this.supplierSearch.next(search);
  }

  protected onFilterChange(): void {
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  protected statusSeverity(status: PayableStatus): 'info' | 'success' | 'secondary' {
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
    this.payableService
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
          this.payables.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingPayable.set(null);
    this.form.reset({
      supplierId: null,
      categoryId: null,
      description: '',
      dueDate: new Date(),
      amount: 0,
      paymentMethodId: null,
      notes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(payable: Payable): void {
    this.editingPayable.set(payable);
    this.ensureSupplierOption(payable);
    this.form.reset({
      supplierId: payable.supplier?.id ?? null,
      categoryId: payable.category?.id ?? null,
      description: payable.description ?? '',
      dueDate: new Date(payable.dueDate),
      amount: payable.amount,
      paymentMethodId: payable.paymentMethod?.id ?? null,
      notes: payable.notes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureSupplierOption(payable: Payable): void {
    const supplier = payable.supplier;
    if (!supplier || this.supplierOptions().some((option) => option.id === supplier.id)) {
      return;
    }
    this.customerService.getById(supplier.id).subscribe((loaded) => {
      this.supplierOptions.set([loaded, ...this.supplierOptions()]);
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
    const request: PayableRequest = {
      supplierId: raw.supplierId!,
      categoryId: raw.categoryId,
      description: trimmedOrNull(raw.description),
      dueDate: toIsoDate(raw.dueDate)!,
      amount: raw.amount,
      paymentMethodId: raw.paymentMethodId,
      notes: trimmedOrNull(raw.notes)
    };

    this.saving.set(true);
    const editing = this.editingPayable();
    const request$ = editing
      ? this.payableService.update(editing.id, request)
      : this.payableService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'payablesPage.messages.updated' : 'payablesPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected openPayDialog(payable: Payable): void {
    this.payingPayable.set(payable);
    this.payForm.reset({ amount: null, paymentMethodId: payable.paymentMethod?.id ?? null });
    this.payDialogVisible.set(true);
  }

  protected closePayDialog(): void {
    this.payDialogVisible.set(false);
  }

  protected confirmPay(): void {
    const payable = this.payingPayable();
    if (!payable) {
      return;
    }
    const raw = this.payForm.getRawValue();

    this.paying.set(true);
    this.payableService
      .pay(payable.id, { amount: raw.amount, paymentMethodId: raw.paymentMethodId })
      .subscribe({
        next: () => {
          this.paying.set(false);
          this.payDialogVisible.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('payablesPage.messages.paid')
          });
          this.load();
        },
        error: () => this.paying.set(false)
      });
  }

  protected confirmDelete(payable: Payable): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: payable.code }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.payableService.delete(payable.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('payablesPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
