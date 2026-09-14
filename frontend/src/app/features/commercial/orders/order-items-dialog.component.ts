import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { OrderItem, OrderItemRequest } from '../../../core/models/order.model';
import { Product } from '../../../core/models/product.model';
import { OrderService } from '../../../core/services/order.service';
import { ProductService } from '../../../core/services/product.service';
import { SessionStore } from '../../../core/store/session.store';
import { formatCurrencyBRL } from '../../../shared/utils/format.util';

const PRODUCT_OPTIONS_SIZE = 200;

@Component({
  selector: 'app-order-items-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    DialogModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    TooltipModule
  ],
  templateUrl: './order-items-dialog.component.html',
  styleUrl: './order-items-dialog.component.scss'
})
export class OrderItemsDialogComponent {
  visible = input(false);
  orderId = input<string | null>(null);
  orderCode = input('');

  visibleChange = output<boolean>();
  itemsChanged = output<void>();

  private readonly orderService = inject(OrderService);
  private readonly productService = inject(ProductService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly items = signal<OrderItem[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly formVisible = signal(false);
  protected readonly editingItem = signal<OrderItem | null>(null);
  protected readonly products = signal<Product[]>([]);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PEDIDOS_EDIT'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PEDIDOS_EDIT'));

  protected readonly itemsTotal = computed(() => this.items().reduce((sum, item) => sum + item.total, 0));

  protected readonly form = this.formBuilder.nonNullable.group({
    productId: [null as string | null, [Validators.required]],
    description: [''],
    quantity: [1, [Validators.required, Validators.min(0.001)]],
    unitPrice: [null as number | null],
    discountPercent: [0, [Validators.min(0), Validators.max(100)]]
  });

  constructor() {
    effect(() => {
      const visible = this.visible();
      const orderId = this.orderId();
      if (visible && orderId) {
        untracked(() => this.load());
      }
    });

    this.productService
      .list({ active: true, size: PRODUCT_OPTIONS_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.products.set(response.content));
  }

  protected formatPrice(value: number): string {
    return formatCurrencyBRL(value);
  }

  private load(): void {
    const orderId = this.orderId();
    if (!orderId) {
      return;
    }
    this.loading.set(true);
    this.orderService.listItems(orderId).subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected close(): void {
    this.visibleChange.emit(false);
  }

  protected openCreateForm(): void {
    this.editingItem.set(null);
    this.form.reset({
      productId: null,
      description: '',
      quantity: 1,
      unitPrice: null,
      discountPercent: 0
    });
    this.formVisible.set(true);
  }

  protected openEditForm(item: OrderItem): void {
    this.editingItem.set(item);
    this.form.reset({
      productId: item.product?.id ?? null,
      description: item.description ?? '',
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      discountPercent: item.discountPercent
    });
    this.formVisible.set(true);
  }

  protected closeForm(): void {
    this.formVisible.set(false);
  }

  protected save(): void {
    const orderId = this.orderId();
    if (!orderId || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: OrderItemRequest = {
      productId: raw.productId!,
      description: raw.description?.trim() ? raw.description.trim() : null,
      quantity: raw.quantity,
      unitPrice: raw.unitPrice,
      discountPercent: raw.discountPercent
    };

    this.saving.set(true);
    const editing = this.editingItem();
    const request$ = editing
      ? this.orderService.updateItem(orderId, editing.id, request)
      : this.orderService.createItem(orderId, request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'ordersPage.items.messages.updated' : 'ordersPage.items.messages.created')
        });
        this.load();
        this.itemsChanged.emit();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(item: OrderItem): void {
    const orderId = this.orderId();
    if (!orderId) {
      return;
    }
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: item.product?.name ?? '' }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.orderService.deleteItem(orderId, item.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('ordersPage.items.messages.deleted')
          });
          this.load();
          this.itemsChanged.emit();
        });
      }
    });
  }
}
