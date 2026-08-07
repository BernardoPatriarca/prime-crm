import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { Role } from '../../../core/models/role.model';
import { RoleService } from '../../../core/services/role.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { RolePermissionsDialogComponent } from './role-permissions-dialog.component';

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

@Component({
  selector: 'app-roles-page',
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
    ToggleSwitchModule,
    TagModule,
    TooltipModule,
    RolePermissionsDialogComponent
  ],
  templateUrl: './roles-page.component.html',
  styleUrl: './roles-page.component.scss'
})
export class RolesPageComponent {
  private readonly roleService = inject(RoleService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly roles = signal<Role[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editingRole = signal<Role | null>(null);
  protected readonly saving = signal(false);

  protected readonly permissionsDialogVisible = signal(false);
  protected readonly permissionsRole = signal<Role | null>(null);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PERFIS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PERFIS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('PERFIS_DELETE'));

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(255)]],
    active: [true]
  });

  constructor() {
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.roleService
      .list({
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.roles.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingRole.set(null);
    this.form.reset({ name: '', description: '', active: true });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(role: Role): void {
    this.editingRole.set(role);
    this.form.reset({
      name: role.name,
      description: role.description ?? '',
      active: role.active
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
      name: raw.name,
      description: raw.description || null,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingRole();
    const request$ = editing ? this.roleService.update(editing.id, request) : this.roleService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'settingsPages.roles.messages.updated' : 'settingsPages.roles.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(role: Role): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: role.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.roleService.delete(role.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.roles.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected openPermissionsDialog(role: Role): void {
    this.permissionsRole.set(role);
    this.permissionsDialogVisible.set(true);
  }

  protected onPermissionsDialogVisibleChange(visible: boolean): void {
    this.permissionsDialogVisible.set(visible);
    if (!visible) {
      this.load();
    }
  }

  protected countLabel(value: number): string {
    return String(value);
  }
}
