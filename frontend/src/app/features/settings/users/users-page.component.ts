import { Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AdminUser } from '../../../core/models/admin-user.model';
import { Role } from '../../../core/models/role.model';
import { UserStatus } from '../../../core/models/user.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { RoleService } from '../../../core/services/role.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirmation = group.get('confirmPassword')?.value;
  return password && confirmation && password !== confirmation ? { passwordMismatch: true } : null;
}

const USER_STATUSES: UserStatus[] = ['ACTIVE', 'INACTIVE', 'BLOCKED'];

@Component({
  selector: 'app-users-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    AvatarModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    MultiSelectModule,
    PasswordModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './users-page.component.html',
  styleUrl: './users-page.component.scss'
})
export class UsersPageComponent {
  private readonly adminUserService = inject(AdminUserService);
  private readonly roleService = inject(RoleService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly statuses = USER_STATUSES;

  protected readonly users = signal<AdminUser[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly statusFilter = signal<UserStatus | null>(null);

  protected readonly dialogVisible = signal(false);
  protected readonly editingUser = signal<AdminUser | null>(null);
  protected readonly saving = signal(false);
  protected readonly roles = signal<Role[]>([]);

  protected readonly resetPasswordDialogVisible = signal(false);
  protected readonly resetPasswordUser = signal<AdminUser | null>(null);
  protected readonly resettingPassword = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('USUARIOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('USUARIOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('USUARIOS_DELETE'));
  protected readonly currentUserId = computed(() => this.sessionStore.user()?.id ?? null);

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
    login: ['', [Validators.required, Validators.maxLength(80)]],
    phone: ['', [Validators.maxLength(30)]],
    password: ['', [Validators.minLength(8), Validators.maxLength(72)]],
    roleIds: [[] as string[]]
  });

  protected readonly resetPasswordForm = this.formBuilder.nonNullable.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmPassword: ['', [Validators.required]]
    },
    { validators: passwordsMatchValidator }
  );

  constructor() {
    this.load();
    this.roleService.list({ size: 200 }).subscribe((response) => this.roles.set(response.content));
  }

  protected onStatusFilterChange(value: UserStatus | null): void {
    this.statusFilter.set(value);
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.adminUserService
      .list({
        status: this.statusFilter() ?? undefined,
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.users.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingUser.set(null);
    this.form.controls.password.setValidators([Validators.required, Validators.minLength(8), Validators.maxLength(72)]);
    this.form.controls.password.updateValueAndValidity();
    this.form.reset({ name: '', email: '', login: '', phone: '', password: '', roleIds: [] });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(user: AdminUser): void {
    this.editingUser.set(user);
    this.form.controls.password.clearValidators();
    this.form.controls.password.updateValueAndValidity();
    this.form.reset({
      name: user.name,
      email: user.email,
      login: user.login,
      phone: user.phone ?? '',
      password: '',
      roleIds: user.roles.map((role) => role.id)
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
    const editing = this.editingUser();
    this.saving.set(true);

    if (editing) {
      this.adminUserService
        .update(editing.id, { name: raw.name, email: raw.email, login: raw.login, phone: raw.phone || null })
        .subscribe({
          next: () => {
            this.adminUserService.assignRoles(editing.id, { roleIds: raw.roleIds }).subscribe({
              next: () => {
                this.saving.set(false);
                this.dialogVisible.set(false);
                this.messageService.add({
                  severity: 'success',
                  summary: this.translate.instant('settingsPages.users.messages.updated')
                });
                this.load();
              },
              error: () => this.saving.set(false)
            });
          },
          error: () => this.saving.set(false)
        });
      return;
    }

    this.adminUserService
      .create({ name: raw.name, email: raw.email, login: raw.login, password: raw.password, phone: raw.phone || null })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.dialogVisible.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.users.messages.created')
          });
          this.load();
        },
        error: () => this.saving.set(false)
      });
  }

  protected confirmDelete(user: AdminUser): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: user.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.adminUserService.delete(user.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.users.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected initials(name: string): string {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  }

  protected statusSeverity(status: UserStatus): 'success' | 'warn' | 'danger' {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'INACTIVE':
        return 'warn';
      default:
        return 'danger';
    }
  }

  protected changeStatus(user: AdminUser, status: UserStatus): void {
    this.adminUserService.updateStatus(user.id, { status }).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('settingsPages.users.messages.statusUpdated')
      });
      this.load();
    });
  }

  protected openResetPasswordDialog(user: AdminUser): void {
    this.resetPasswordUser.set(user);
    this.resetPasswordForm.reset({ newPassword: '', confirmPassword: '' });
    this.resetPasswordDialogVisible.set(true);
  }

  protected closeResetPasswordDialog(): void {
    this.resetPasswordDialogVisible.set(false);
  }

  protected submitResetPassword(): void {
    const user = this.resetPasswordUser();
    if (!user || this.resetPasswordForm.invalid) {
      this.resetPasswordForm.markAllAsTouched();
      return;
    }

    this.resettingPassword.set(true);
    this.adminUserService
      .changePassword(user.id, { newPassword: this.resetPasswordForm.getRawValue().newPassword })
      .subscribe({
        next: () => {
          this.resettingPassword.set(false);
          this.resetPasswordDialogVisible.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.users.messages.passwordReset')
          });
        },
        error: () => this.resettingPassword.set(false)
      });
  }
}
