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
import { ORDER_STATUSES, Order, OrderRequest, OrderStatus } from '../../../core/models/order.model';
import { Opportunity } from '../../../core/models/opportunity.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { ContractService } from '../../../core/services/contract.service';
import { CustomerService } from '../../../core/services/customer.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { OrderService } from '../../../core/services/order.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../../shared/utils/creation-route.util';
import { downloadBlob } from '../../../shared/utils/file-download.util';
import { formatCurrencyBRL, formatIsoDate } from '../../../shared/utils/format.util';
import { OrderItemsDialogComponent } from './order-items-dialog.component';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const OPTIONS_PAGE_SIZE = 50;

const STATUS_SEVERITY: Record<OrderStatus, 'secondary' | 'info' | 'success' | 'danger'> = {
  PENDING: 'secondary',
  CONFIRMED: 'info',
  DELIVERED: 'success',
  CANCELED: 'danger'
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
  selector: 'app-orders-page',
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
    OrderItemsDialogComponent
  ],
  templateUrl: './orders-page.component.html',
  styleUrl: './orders-page.component.scss'
})
export class OrdersPageComponent {
  private readonly orderService = inject(OrderService);
  private readonly contractService = inject(ContractService);
  private readonly customerService = inject(CustomerService);
  private readonly opportunityService = inject(OpportunityService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly orders = signal<Order[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly statusFilter = signal<OrderStatus | null>(null);

  protected readonly customerOptions = signal<Customer[]>([]);
  protected readonly opportunityOptions = signal<Opportunity[]>([]);
  protected readonly userOptions = signal<AdminUser[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingOrder = signal<Order | null>(null);
  protected readonly saving = signal(false);

  protected readonly itemsDialogVisible = signal(false);
  protected readonly orderForItems = signal<Order | null>(null);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PEDIDOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PEDIDOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('PEDIDOS_DELETE'));
  protected readonly canCreateContract = computed(() => this.sessionStore.hasPermission('CONTRATOS_CREATE'));

  protected readonly statusOptions = computed<SelectOption<OrderStatus>[]>(() => {
    this.translate.currentLang();
    return ORDER_STATUSES.map((status) => ({
      label: this.translate.instant(`ordersPage.status.${status}`),
      value: status
    }));
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    customerId: [null as string | null, [Validators.required]],
    opportunityId: [null as string | null],
    ownerUserId: [null as string | null],
    orderDate: [new Date(), [Validators.required]],
    deliveryDate: [null as Date | null],
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

  protected statusSeverity(status: OrderStatus): string {
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
    this.orderService
      .list({
        search: this.lastQuery.search,
        status: this.statusFilter() ?? undefined,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.orders.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingOrder.set(null);
    this.form.reset({
      customerId: null,
      opportunityId: null,
      ownerUserId: null,
      orderDate: new Date(),
      deliveryDate: null,
      notes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(order: Order): void {
    this.editingOrder.set(order);
    this.ensureCustomerOption(order);
    this.form.reset({
      customerId: order.customer?.id ?? null,
      opportunityId: order.opportunity?.id ?? null,
      ownerUserId: order.owner?.id ?? null,
      orderDate: new Date(order.orderDate),
      deliveryDate: order.deliveryDate ? new Date(order.deliveryDate) : null,
      notes: order.notes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(order: Order): void {
    const customer = order.customer;
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
    const request: OrderRequest = {
      customerId: raw.customerId!,
      opportunityId: raw.opportunityId,
      ownerUserId: raw.ownerUserId,
      orderDate: toIsoDate(raw.orderDate),
      deliveryDate: toIsoDate(raw.deliveryDate),
      notes: trimmedOrNull(raw.notes)
    };

    this.saving.set(true);
    const editing = this.editingOrder();
    const request$ = editing ? this.orderService.update(editing.id, request) : this.orderService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'ordersPage.messages.updated' : 'ordersPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected changeStatus(order: Order, status: OrderStatus): void {
    this.orderService.changeStatus(order.id, { status }).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('ordersPage.messages.statusChanged')
      });
      this.load();
    });
  }

  protected convertToContract(order: Order): void {
    this.contractService.createFromOrder(order.id).subscribe((contract) => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('ordersPage.messages.convertedToContract', { code: contract.code })
      });
    });
  }

  protected downloadPdf(order: Order): void {
    this.orderService.pdf(order.id).subscribe((blob) => downloadBlob(blob, `${order.code}.pdf`));
  }

  protected openItemsDialog(order: Order): void {
    this.orderForItems.set(order);
    this.itemsDialogVisible.set(true);
  }

  protected onItemsChanged(): void {
    this.load();
  }

  protected confirmDelete(order: Order): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: order.code }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.orderService.delete(order.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('ordersPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
