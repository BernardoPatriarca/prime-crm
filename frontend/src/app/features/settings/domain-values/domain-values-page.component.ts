import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ColorPickerModule } from 'primeng/colorpicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule, TableRowReorderEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { DomainType, DomainValue } from '../../../core/models/domain-value.model';
import { DomainTypeService } from '../../../core/services/domain-type.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';

const ICON_OPTIONS = [
  'pi-tag',
  'pi-tags',
  'pi-star',
  'pi-flag',
  'pi-bookmark',
  'pi-briefcase',
  'pi-building',
  'pi-users',
  'pi-user',
  'pi-shopping-cart',
  'pi-dollar',
  'pi-chart-line',
  'pi-thumbs-up',
  'pi-thumbs-down',
  'pi-exclamation-circle',
  'pi-check-circle',
  'pi-clock',
  'pi-calendar',
  'pi-map-marker',
  'pi-phone',
  'pi-envelope',
  'pi-box',
  'pi-folder',
  'pi-globe',
  'pi-shield'
];

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

@Component({
  selector: 'app-domain-values-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    ColorPickerModule,
    SelectModule,
    InputNumberModule,
    ToggleSwitchModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './domain-values-page.component.html',
  styleUrl: './domain-values-page.component.scss'
})
export class DomainValuesPageComponent {
  tipo = input.required<string>();

  private readonly domainTypeService = inject(DomainTypeService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly iconOptions = ICON_OPTIONS;

  protected readonly domainType = signal<DomainType | null>(null);
  protected readonly domainTypeLabel = computed(() => this.domainType()?.label ?? this.tipo());

  protected readonly values = signal<DomainValue[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editingValue = signal<DomainValue | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('DOMINIOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('DOMINIOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('DOMINIOS_DELETE'));

  protected lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    code: ['', [Validators.maxLength(80)]],
    description: [''],
    color: ['#3B82F6'],
    icon: [''],
    displayOrder: [0],
    active: [true]
  });

  constructor() {
    effect(() => {
      const tipo = this.tipo();
      untracked(() => {
        this.loadDomainType(tipo);
        this.lastQuery = { page: 0, size: this.lastQuery.size };
        this.load();
      });
    });
  }

  private loadDomainType(tipo: string): void {
    this.domainTypeService.list().subscribe((types) => {
      this.domainType.set(types.find((t) => t.code === tipo) ?? null);
    });
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.domainValueService
      .list({
        type: this.tipo(),
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.values.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingValue.set(null);
    this.form.reset({
      name: '',
      code: '',
      description: '',
      color: '#3B82F6',
      icon: '',
      displayOrder: this.values().length,
      active: true
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(value: DomainValue): void {
    this.editingValue.set(value);
    this.form.reset({
      name: value.name,
      code: value.code ?? '',
      description: value.description ?? '',
      color: value.color ?? '#3B82F6',
      icon: value.icon ?? '',
      displayOrder: value.displayOrder,
      active: value.active
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
    const supportsColor = this.domainType()?.supportsColor ?? true;
    const supportsIcon = this.domainType()?.supportsIcon ?? true;

    const request = {
      domainTypeCode: this.tipo(),
      name: raw.name,
      code: raw.code || null,
      description: raw.description || null,
      color: supportsColor ? raw.color || null : null,
      icon: supportsIcon ? raw.icon || null : null,
      displayOrder: raw.displayOrder,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingValue();
    const request$ = editing
      ? this.domainValueService.update(editing.id, request)
      : this.domainValueService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'settingsPages.domainValues.messages.updated' : 'settingsPages.domainValues.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(value: DomainValue): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: value.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => this.delete(value)
    });
  }

  private delete(value: DomainValue): void {
    this.domainValueService.delete(value.id).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('settingsPages.domainValues.messages.deleted')
      });
      this.load();
    });
  }

  protected onRowReorder(event: TableRowReorderEvent): void {
    if (event.dragIndex === undefined || event.dropIndex === undefined) {
      return;
    }
    const baseOrder = (this.lastQuery.page ?? 0) * (this.lastQuery.size ?? 10);
    const items = this.values().map((value, index) => ({
      id: value.id,
      displayOrder: baseOrder + index
    }));
    this.domainValueService.reorder(items).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('settingsPages.domainValues.messages.reordered')
      });
      this.load();
    });
  }
}
