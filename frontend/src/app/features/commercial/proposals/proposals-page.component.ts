import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AdminUser } from '../../../core/models/admin-user.model';
import { Customer } from '../../../core/models/customer.model';
import { Opportunity } from '../../../core/models/opportunity.model';
import { PROPOSAL_STATUSES, Proposal, ProposalRequest, ProposalStatus } from '../../../core/models/proposal.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { CustomerService } from '../../../core/services/customer.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { ProposalService } from '../../../core/services/proposal.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../../shared/utils/creation-route.util';
import { formatCurrencyBRL, formatIsoDate } from '../../../shared/utils/format.util';
import { ProposalItemsDialogComponent } from './proposal-items-dialog.component';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const OPTIONS_PAGE_SIZE = 50;

const STATUS_SEVERITY: Record<ProposalStatus, 'secondary' | 'info' | 'success' | 'danger'> = {
  DRAFT: 'secondary',
  SENT: 'info',
  ACCEPTED: 'success',
  REJECTED: 'danger'
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
  selector: 'app-proposals-page',
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
    TagModule,
    TextareaModule,
    TooltipModule,
    SharedModule,
    ProposalItemsDialogComponent
  ],
  templateUrl: './proposals-page.component.html',
  styleUrl: './proposals-page.component.scss'
})
export class ProposalsPageComponent {
  private readonly proposalService = inject(ProposalService);
  private readonly customerService = inject(CustomerService);
  private readonly opportunityService = inject(OpportunityService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly proposals = signal<Proposal[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly statusFilter = signal<ProposalStatus | null>(null);
  protected readonly expiredFilter = signal<boolean | null>(null);

  protected readonly customerOptions = signal<Customer[]>([]);
  protected readonly opportunityOptions = signal<Opportunity[]>([]);
  protected readonly userOptions = signal<AdminUser[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingProposal = signal<Proposal | null>(null);
  protected readonly saving = signal(false);

  protected readonly itemsDialogVisible = signal(false);
  protected readonly proposalForItems = signal<Proposal | null>(null);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PROPOSTAS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PROPOSTAS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('PROPOSTAS_DELETE'));

  protected readonly statusOptions = computed<SelectOption<ProposalStatus>[]>(() => {
    this.translate.currentLang();
    return PROPOSAL_STATUSES.map((status) => ({
      label: this.translate.instant(`proposalsPage.status.${status}`),
      value: status
    }));
  });

  protected readonly expiredOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('proposalsPage.filters.expired'), value: true },
      { label: this.translate.instant('proposalsPage.filters.valid'), value: false }
    ];
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    customerId: [null as string | null, [Validators.required]],
    contactId: [null as string | null],
    opportunityId: [null as string | null],
    ownerUserId: [null as string | null],
    issueDate: [new Date(), [Validators.required]],
    validUntil: [null as Date | null],
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

  protected statusSeverity(status: ProposalStatus): string {
    return STATUS_SEVERITY[status];
  }

  protected formatDate(value: string | null): string {
    return formatIsoDate(value);
  }

  protected formatTotal(value: number): string {
    return formatCurrencyBRL(value);
  }

  private load(): void {
    this.loading.set(true);
    const expired = this.expiredFilter();
    this.proposalService
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
          this.proposals.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingProposal.set(null);
    this.form.reset({
      customerId: null,
      contactId: null,
      opportunityId: null,
      ownerUserId: null,
      issueDate: new Date(),
      validUntil: null,
      notes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(proposal: Proposal): void {
    this.editingProposal.set(proposal);
    this.ensureCustomerOption(proposal);
    this.form.reset({
      customerId: proposal.customer?.id ?? null,
      contactId: proposal.contact?.id ?? null,
      opportunityId: proposal.opportunity?.id ?? null,
      ownerUserId: proposal.owner?.id ?? null,
      issueDate: new Date(proposal.issueDate),
      validUntil: proposal.validUntil ? new Date(proposal.validUntil) : null,
      notes: proposal.notes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(proposal: Proposal): void {
    const customer = proposal.customer;
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
    const request: ProposalRequest = {
      customerId: raw.customerId!,
      contactId: raw.contactId,
      opportunityId: raw.opportunityId,
      ownerUserId: raw.ownerUserId,
      issueDate: toIsoDate(raw.issueDate),
      validUntil: toIsoDate(raw.validUntil),
      notes: trimmedOrNull(raw.notes)
    };

    this.saving.set(true);
    const editing = this.editingProposal();
    const request$ = editing ? this.proposalService.update(editing.id, request) : this.proposalService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'proposalsPage.messages.updated' : 'proposalsPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected changeStatus(proposal: Proposal, status: ProposalStatus): void {
    this.proposalService.changeStatus(proposal.id, { status }).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('proposalsPage.messages.statusChanged')
      });
      this.load();
    });
  }

  protected openItemsDialog(proposal: Proposal): void {
    this.proposalForItems.set(proposal);
    this.itemsDialogVisible.set(true);
  }

  protected onItemsChanged(): void {
    this.load();
  }

  protected confirmDelete(proposal: Proposal): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: proposal.code }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.proposalService.delete(proposal.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('proposalsPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
