import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { DomainValue } from '../../../core/models/domain-value.model';
import { Product, ProductRequest } from '../../../core/models/product.model';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { ProductService } from '../../../core/services/product.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { openCreateDialogFromRoute } from '../../../shared/utils/creation-route.util';
import { formatCurrencyBRL } from '../../../shared/utils/format.util';

const DOMAIN_OPTIONS_SIZE = 200;

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
  selector: 'app-products-page',
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
    CheckboxModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './products-page.component.html',
  styleUrl: './products-page.component.scss'
})
export class ProductsPageComponent {
  private readonly productService = inject(ProductService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly products = signal<Product[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly categoryFilter = signal<string | null>(null);
  protected readonly serviceFilter = signal<boolean | null>(null);
  protected readonly activeFilter = signal<boolean | null>(null);

  protected readonly categories = signal<DomainValue[]>([]);
  protected readonly units = signal<DomainValue[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingProduct = signal<Product | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PRODUTOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PRODUTOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('PRODUTOS_DELETE'));

  protected readonly typeOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('productsPage.filters.product'), value: false },
      { label: this.translate.instant('productsPage.filters.service'), value: true }
    ];
  });

  protected readonly activeOptions = computed<SelectOption<boolean>[]>(() => {
    this.translate.currentLang();
    return [
      { label: this.translate.instant('productsPage.filters.activeOnly'), value: true },
      { label: this.translate.instant('productsPage.filters.inactiveOnly'), value: false }
    ];
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    sku: ['', [Validators.maxLength(60)]],
    categoryId: [null as string | null],
    unitId: [null as string | null],
    unitPrice: [0, [Validators.required, Validators.min(0)]],
    costPrice: [null as number | null],
    service: [false],
    active: [true]
  });

  constructor() {
    openCreateDialogFromRoute(() => {
      if (this.canCreate()) {
        this.openCreateDialog();
      }
    });

    this.load();
    this.domainValueService
      .list({ type: 'CATEGORY', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.categories.set(response.content));
    this.domainValueService
      .list({ type: 'UNIT_OF_MEASURE', active: true, size: DOMAIN_OPTIONS_SIZE, sort: 'displayOrder,asc' })
      .subscribe((response) => this.units.set(response.content));
  }

  protected onFilterChange(): void {
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  protected formatPrice(value: number | null): string {
    return formatCurrencyBRL(value);
  }

  private load(): void {
    this.loading.set(true);
    this.productService
      .list({
        search: this.lastQuery.search,
        categoryId: this.categoryFilter() ?? undefined,
        service: this.serviceFilter() ?? undefined,
        active: this.activeFilter() ?? undefined,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.products.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingProduct.set(null);
    this.form.reset({
      name: '',
      description: '',
      sku: '',
      categoryId: null,
      unitId: null,
      unitPrice: 0,
      costPrice: null,
      service: false,
      active: true
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(product: Product): void {
    this.editingProduct.set(product);
    this.form.reset({
      name: product.name,
      description: product.description ?? '',
      sku: product.sku ?? '',
      categoryId: product.category?.id ?? null,
      unitId: product.unit?.id ?? null,
      unitPrice: product.unitPrice,
      costPrice: product.costPrice,
      service: product.service,
      active: product.active
    });
    this.dialogVisible.set(true);
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
    const request: ProductRequest = {
      name: raw.name.trim(),
      description: trimmedOrNull(raw.description),
      sku: trimmedOrNull(raw.sku),
      categoryId: raw.categoryId,
      unitId: raw.unitId,
      unitPrice: raw.unitPrice,
      costPrice: raw.costPrice,
      service: raw.service,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingProduct();
    const request$ = editing
      ? this.productService.update(editing.id, request)
      : this.productService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(editing ? 'productsPage.messages.updated' : 'productsPage.messages.created')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(product: Product): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: product.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.productService.delete(product.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('productsPage.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}
