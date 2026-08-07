import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { CustomField, FIELD_TYPES, FieldType } from '../../../core/models/custom-field.model';
import { CustomFieldService } from '../../../core/services/custom-field.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

function optionsToEntries(options: Record<string, unknown> | null): { key: string; value: string }[] {
  if (!options) {
    return [];
  }
  return Object.entries(options).map(([key, value]) => ({ key, value: String(value) }));
}

function createOptionGroup(formBuilder: FormBuilder, key = '', value = '') {
  return formBuilder.nonNullable.group({
    key: [key, [Validators.required]],
    value: [value, [Validators.required]]
  });
}

type OptionGroup = ReturnType<typeof createOptionGroup>;

@Component({
  selector: 'app-custom-fields-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    ToggleSwitchModule,
    TagModule,
    TooltipModule
  ],
  templateUrl: './custom-fields-page.component.html',
  styleUrl: './custom-fields-page.component.scss'
})
export class CustomFieldsPageComponent {
  private readonly customFieldService = inject(CustomFieldService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly fieldTypes = FIELD_TYPES;

  protected readonly fields = signal<CustomField[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editingField = signal<CustomField | null>(null);
  protected readonly saving = signal(false);
  protected readonly targetEntityFilter = signal('');

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('CAMPOS_PERSONALIZADOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('CAMPOS_PERSONALIZADOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('CAMPOS_PERSONALIZADOS_DELETE'));

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private filterDebounce?: ReturnType<typeof setTimeout>;

  protected readonly form = this.formBuilder.nonNullable.group({
    targetEntity: ['', [Validators.required, Validators.maxLength(60)]],
    fieldKey: ['', [Validators.required, Validators.maxLength(100)]],
    label: ['', [Validators.required, Validators.maxLength(150)]],
    fieldType: ['TEXT' as FieldType, [Validators.required]],
    required: [false],
    displayOrder: [0],
    active: [true],
    options: this.formBuilder.array<OptionGroup>([])
  });

  constructor() {
    this.load();
  }

  protected get optionsArray(): FormArray<OptionGroup> {
    return this.form.controls.options;
  }

  protected showOptionsEditor(): boolean {
    const type = this.form.controls.fieldType.value;
    return type === 'SELECT' || type === 'MULTISELECT';
  }

  protected addOption(): void {
    this.optionsArray.push(createOptionGroup(this.formBuilder));
  }

  protected removeOption(index: number): void {
    this.optionsArray.removeAt(index);
  }

  protected onTargetEntityFilterChange(value: string): void {
    this.targetEntityFilter.set(value);
    if (this.filterDebounce) {
      clearTimeout(this.filterDebounce);
    }
    this.filterDebounce = setTimeout(() => {
      this.lastQuery = { ...this.lastQuery, page: 0 };
      this.load();
    }, 350);
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.customFieldService
      .list({
        targetEntity: this.targetEntityFilter().trim() || undefined,
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.fields.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingField.set(null);
    this.optionsArray.clear();
    this.form.reset({
      targetEntity: this.targetEntityFilter().trim(),
      fieldKey: '',
      label: '',
      fieldType: 'TEXT',
      required: false,
      displayOrder: this.fields().length,
      active: true
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(field: CustomField): void {
    this.editingField.set(field);
    this.optionsArray.clear();
    optionsToEntries(field.options).forEach((entry) => {
      this.optionsArray.push(createOptionGroup(this.formBuilder, entry.key, entry.value));
    });
    this.form.reset({
      targetEntity: field.targetEntity,
      fieldKey: field.fieldKey,
      label: field.label,
      fieldType: field.fieldType,
      required: field.required,
      displayOrder: field.displayOrder,
      active: field.active
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
    const options = this.showOptionsEditor()
      ? Object.fromEntries(raw.options.map((entry) => [entry.key, entry.value]))
      : null;

    const request = {
      targetEntity: raw.targetEntity,
      fieldKey: raw.fieldKey,
      label: raw.label,
      fieldType: raw.fieldType,
      options,
      required: raw.required,
      displayOrder: raw.displayOrder,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingField();
    const request$ = editing
      ? this.customFieldService.update(editing.id, request)
      : this.customFieldService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'settingsPages.customFields.messages.updated' : 'settingsPages.customFields.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(field: CustomField): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: field.label }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.customFieldService.delete(field.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.customFields.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected fieldTypeIcon(fieldType: FieldType): string {
    switch (fieldType) {
      case 'NUMBER':
        return 'pi-hashtag';
      case 'DATE':
        return 'pi-calendar';
      case 'SELECT':
        return 'pi-chevron-circle-down';
      case 'MULTISELECT':
        return 'pi-list';
      case 'BOOLEAN':
        return 'pi-check-square';
      default:
        return 'pi-align-left';
    }
  }
}
