import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormsModule, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { MessageTemplate, TEMPLATE_TYPES, TemplateType } from '../../../core/models/template.model';
import { TemplateService } from '../../../core/services/template.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

function emailSubjectValidator(group: AbstractControl): ValidationErrors | null {
  const type = group.get('type')?.value;
  const subject = (group.get('subject')?.value ?? '').trim();
  return type === 'EMAIL' && !subject ? { subjectRequiredForEmail: true } : null;
}

@Component({
  selector: 'app-templates-page',
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
    InputTextModule,
    SelectModule,
    TextareaModule,
    ToggleSwitchModule,
    TagModule,
    TooltipModule
  ],
  templateUrl: './templates-page.component.html',
  styleUrl: './templates-page.component.scss'
})
export class TemplatesPageComponent {
  private readonly templateService = inject(TemplateService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly templateTypes = TEMPLATE_TYPES;

  protected readonly templates = signal<MessageTemplate[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editingTemplate = signal<MessageTemplate | null>(null);
  protected readonly saving = signal(false);
  protected readonly typeFilter = signal<TemplateType | null>(null);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('TEMPLATES_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('TEMPLATES_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('TEMPLATES_DELETE'));

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group(
    {
      type: ['EMAIL' as TemplateType, [Validators.required]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      subject: ['', [Validators.maxLength(255)]],
      content: ['', [Validators.required]],
      active: [true]
    },
    { validators: emailSubjectValidator }
  );

  constructor() {
    this.load();
  }

  protected onTypeFilterChange(value: TemplateType | null): void {
    this.typeFilter.set(value);
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.templateService
      .list({
        type: this.typeFilter() ?? undefined,
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.templates.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingTemplate.set(null);
    this.form.reset({
      type: this.typeFilter() ?? 'EMAIL',
      name: '',
      subject: '',
      content: '',
      active: true
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(template: MessageTemplate): void {
    this.editingTemplate.set(template);
    this.form.reset({
      type: template.type,
      name: template.name,
      subject: template.subject ?? '',
      content: template.content,
      active: template.active
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
    const request = {
      type: raw.type,
      name: raw.name,
      subject: raw.type === 'EMAIL' ? raw.subject : null,
      content: raw.content,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingTemplate();
    const request$ = editing ? this.templateService.update(editing.id, request) : this.templateService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'settingsPages.templates.messages.updated' : 'settingsPages.templates.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(template: MessageTemplate): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: template.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.templateService.delete(template.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.templates.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected typeIcon(type: TemplateType): string {
    switch (type) {
      case 'EMAIL':
        return 'pi-envelope';
      case 'PROPOSAL':
        return 'pi-file-edit';
      case 'CONTRACT':
        return 'pi-file-check';
      default:
        return 'pi-comments';
    }
  }

  protected typeSeverity(type: TemplateType): 'info' | 'warn' | 'success' | 'secondary' {
    switch (type) {
      case 'EMAIL':
        return 'info';
      case 'PROPOSAL':
        return 'warn';
      case 'CONTRACT':
        return 'success';
      default:
        return 'secondary';
    }
  }
}
