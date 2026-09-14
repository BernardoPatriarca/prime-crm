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
import { Product } from '../../../core/models/product.model';
import { ProposalItem, ProposalItemRequest } from '../../../core/models/proposal.model';
import { ProductService } from '../../../core/services/product.service';
import { ProposalService } from '../../../core/services/proposal.service';
import { SessionStore } from '../../../core/store/session.store';
import { formatCurrencyBRL } from '../../../shared/utils/format.util';

const PRODUCT_OPTIONS_SIZE = 200;

@Component({
  selector: 'app-proposal-items-dialog',
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
  templateUrl: './proposal-items-dialog.component.html',
  styleUrl: './proposal-items-dialog.component.scss'
})
export class ProposalItemsDialogComponent {
  visible = input(false);
  proposalId = input<string | null>(null);
  proposalCode = input('');

  visibleChange = output<boolean>();
  itemsChanged = output<void>();

  private readonly proposalService = inject(ProposalService);
  private readonly productService = inject(ProductService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly items = signal<ProposalItem[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly formVisible = signal(false);
  protected readonly editingItem = signal<ProposalItem | null>(null);
  protected readonly products = signal<Product[]>([]);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PROPOSTAS_EDIT'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PROPOSTAS_EDIT'));

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
      const proposalId = this.proposalId();
      if (visible && proposalId) {
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
    const proposalId = this.proposalId();
    if (!proposalId) {
      return;
    }
    this.loading.set(true);
    this.proposalService.listItems(proposalId).subscribe({
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

  protected openEditForm(item: ProposalItem): void {
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
    const proposalId = this.proposalId();
    if (!proposalId || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: ProposalItemRequest = {
      productId: raw.productId!,
      description: raw.description?.trim() ? raw.description.trim() : null,
      quantity: raw.quantity,
      unitPrice: raw.unitPrice,
      discountPercent: raw.discountPercent
    };

    this.saving.set(true);
    const editing = this.editingItem();
    const request$ = editing
      ? this.proposalService.updateItem(proposalId, editing.id, request)
      : this.proposalService.createItem(proposalId, request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'proposalsPage.items.messages.updated' : 'proposalsPage.items.messages.created')
        });
        this.load();
        this.itemsChanged.emit();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(item: ProposalItem): void {
    const proposalId = this.proposalId();
    if (!proposalId) {
      return;
    }
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: item.product?.name ?? '' }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.proposalService.deleteItem(proposalId, item.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('proposalsPage.items.messages.deleted')
          });
          this.load();
          this.itemsChanged.emit();
        });
      }
    });
  }
}
