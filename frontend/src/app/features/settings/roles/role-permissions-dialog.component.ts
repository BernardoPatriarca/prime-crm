import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogModule } from 'primeng/dialog';
import { TagModule } from 'primeng/tag';
import { Permission } from '../../../core/models/permission.model';
import { Role } from '../../../core/models/role.model';
import { PermissionService } from '../../../core/services/permission.service';
import { RoleService } from '../../../core/services/role.service';

interface PermissionGroup {
  module: string;
  permissions: Permission[];
}

@Component({
  selector: 'app-role-permissions-dialog',
  standalone: true,
  imports: [FormsModule, TranslatePipe, DialogModule, ButtonModule, CheckboxModule, TagModule],
  templateUrl: './role-permissions-dialog.component.html',
  styleUrl: './role-permissions-dialog.component.scss'
})
export class RolePermissionsDialogComponent {
  visible = input(false);
  role = input<Role | null>(null);

  visibleChange = output<boolean>();

  private readonly permissionService = inject(PermissionService);
  private readonly roleService = inject(RoleService);
  private readonly messageService = inject(MessageService);
  private readonly translate = inject(TranslateService);

  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  private readonly allPermissions = signal<Permission[]>([]);
  protected readonly selectedIds = signal<Set<string>>(new Set());

  protected readonly groups = computed<PermissionGroup[]>(() => {
    const byModule = new Map<string, Permission[]>();
    for (const permission of this.allPermissions()) {
      const list = byModule.get(permission.module) ?? [];
      list.push(permission);
      byModule.set(permission.module, list);
    }
    return Array.from(byModule.entries())
      .map(([module, permissions]) => ({ module, permissions }))
      .sort((a, b) => a.module.localeCompare(b.module));
  });

  constructor() {
    effect(() => {
      if (this.visible()) {
        this.loadPermissions();
        const role = this.role();
        this.selectedIds.set(new Set(role?.permissions.map((permission) => permission.id) ?? []));
      }
    });
  }

  private loadPermissions(): void {
    this.loading.set(true);
    this.permissionService.list().subscribe({
      next: (permissions) => {
        this.allPermissions.set(permissions);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected isSelected(id: string): boolean {
    return this.selectedIds().has(id);
  }

  protected toggle(id: string, checked: boolean): void {
    const next = new Set(this.selectedIds());
    if (checked) {
      next.add(id);
    } else {
      next.delete(id);
    }
    this.selectedIds.set(next);
  }

  protected selectedCount(group: PermissionGroup): number {
    return group.permissions.filter((permission) => this.isSelected(permission.id)).length;
  }

  protected isGroupFullySelected(group: PermissionGroup): boolean {
    return group.permissions.every((permission) => this.isSelected(permission.id));
  }

  protected toggleGroup(group: PermissionGroup, checked: boolean): void {
    const next = new Set(this.selectedIds());
    for (const permission of group.permissions) {
      if (checked) {
        next.add(permission.id);
      } else {
        next.delete(permission.id);
      }
    }
    this.selectedIds.set(next);
  }

  protected close(): void {
    this.visibleChange.emit(false);
  }

  protected save(): void {
    const role = this.role();
    if (!role) {
      return;
    }
    this.saving.set(true);
    this.roleService.assignPermissions(role.id, { permissionIds: Array.from(this.selectedIds()) }).subscribe({
      next: () => {
        this.saving.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant('settingsPages.roles.permissionsDialog.messages.updated')
        });
        this.visibleChange.emit(false);
      },
      error: () => this.saving.set(false)
    });
  }
}
