import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AdminUser } from '../../../core/models/admin-user.model';
import { CONTRACT_STATUSES, Contract, ContractRequest, ContractStatus } from '../../../core/models/contract.model';
import { Customer } from '../../../core/models/customer.model';
import { DomainValue } from '../../../core/models/domain-value.model';
import { Opportunity } from '../../../core/models/opportunity.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { ContractService } from '../../../core/services/contract.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../../shared/utils/creation-route.util';
import { downloadBlob } from '../../../shared/utils/file-download.util';
import { formatCurrencyBRL, formatIsoDate } from '../../../shared/utils/format.util';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const OPTIONS_PAGE_SIZE = 50;
const DOMAIN_OPTIONS_SIZE = 200;

const STATUS_SEVERITY: Record<ContractStatus, 'secondary' | 'info' | 'success' | 'warn' | 'danger'> = {
  DRAFT: 'secondary',
  ACTIVE: 'success',
  SUSPENDED: 'warn',
  TERMINATED: 'danger'
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
  selector: 'app-contracts-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    SelectModule,
    DatePickerModule,
    InputNumberModule,
    CheckboxModule,
    TagModule,
    TextareaModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './contracts-page.component.html',
  styleUrl: './contracts-page.component.scss'
})
export class ContractsPageComponent {
  private readonly contractService = inject(ContractService);
  private readonly customerService = inject(CustomerService);
  private readonly opportunityService = inject(OpportunityService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly contracts = signal<Contract[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly statusFilter = signal<ContractStatus | null>(null);
  protected readonly expiredFilter = signal<boolean | null>(null);

  protected readonly customerOptions = signal<Customer[]>([]);
  protected readonly opportunityOptions = signal<Opportunity[]>([]);
  protected readonly userOptions = signal<AdminUser[]>([]);
  protected readonly billingCycles = signal<DomainValue[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingContract = signal<Contract | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('CONTRATOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('CONTRATOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('CONTRATOS_DELETE'));

  protected readonly statusOptions = computed<SelectOption<ContractStatus>[]>(() => {
    this.translate.currentLang();
    return CONTRACT_STATUSES.map((status) => ({
      label: this.translate.instant(`contractsPage.status.${status}`),
      value: status
    }));
  });

  protected readonly expiredOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('contractsPage.filters.expired'), value: true },
      { label: this.translate.instant('contractsPage.filters.valid'), value: false }
    ];
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    customerId: [null as string | null, [Validators.required]],
    opportunityId: [null as string | null],
    ownerUserId: [null as string | null],
    billingCycleId: [null as string | null],
    startDate: [new Date(), [Validators.required]],
    endDate: [null as Date | null],
    autoRenew: [false],
    recurringAmount: [0, [Validators.min(0)]],
    notes: ['']
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
    this.opportunityService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'title,asc' })
      .subscribe((response) => this.opportunityOptions.set(response.content));
    this.adminUserService
      .list({ size: OPTIONS_PAGE_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.userOptions.set(response.content));
    this.domainValueService
      .list({ type: 'BILLING_CYCLE', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.billingCycles.set(response.content));
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

  protected statusSeverity(status: ContractStatus): string {
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
    const expired = this.expiredFilter();
    this.contractService
      .list({
        search: this.lastQuery.search,
        status: this.statusFilter() ?? undefined,
        expired: expired === null ? undefined : expired,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.contracts.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingContract.set(null);
    this.form.reset({
      customerId: null,
      opportunityId: null,
      ownerUserId: null,
      billingCycleId: null,
      startDate: new Date(),
      endDate: null,
      autoRenew: false,
      recurringAmount: 0,
      notes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(contract: Contract): void {
    this.editingContract.set(contract);
    this.ensureCustomerOption(contract);
    this.form.reset({
      customerId: contract.customer?.id ?? null,
      opportunityId: contract.opportunity?.id ?? null,
      ownerUserId: contract.owner?.id ?? null,
      billingCycleId: contract.billingCycle?.id ?? null,
      startDate: new Date(contract.startDate),
      endDate: contract.endDate ? new Date(contract.endDate) : null,
      autoRenew: contract.autoRenew,
      recurringAmount: contract.recurringAmount,
      notes: contract.notes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(contract: Contract): void {
    const customer = contract.customer;
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
    const request: ContractRequest = {
      customerId: raw.customerId!,
      opportunityId: raw.opportunityId,
      ownerUserId: raw.ownerUserId,
      billingCycleId: raw.billingCycleId,
      startDate: toIsoDate(raw.startDate),
      endDate: toIsoDate(raw.endDate),
      autoRenew: raw.autoRenew,
      recurringAmount: raw.recurringAmount,
      notes: trimmedOrNull(raw.notes)
    };

    this.saving.set(true);
    const editing = this.editingContract();
    const request$ = editing
      ? this.contractService.update(editing.id, request)
      : this.contractService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'contractsPage.messages.updated' : 'contractsPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected changeStatus(contract: Contract, status: ContractStatus): void {
    this.contractService.changeStatus(contract.id, { status }).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('contractsPage.messages.statusChanged')
      });
      this.load();
    });
  }

  protected downloadPdf(contract: Contract): void {
    this.contractService.pdf(contract.id).subscribe((blob) => downloadBlob(blob, `${contract.code}.pdf`));
  }

  protected confirmDelete(contract: Contract): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: contract.code }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.contractService.delete(contract.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('contractsPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
