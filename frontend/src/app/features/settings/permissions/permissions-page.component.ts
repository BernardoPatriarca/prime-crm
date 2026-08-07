import { Component, computed, inject, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { Permission } from '../../../core/models/permission.model';
import { PermissionService } from '../../../core/services/permission.service';

interface PermissionGroup {
  module: string;
  permissions: Permission[];
}

@Component({
  selector: 'app-permissions-page',
  standalone: true,
  imports: [TranslatePipe, SkeletonModule, TagModule],
  templateUrl: './permissions-page.component.html',
  styleUrl: './permissions-page.component.scss'
})
export class PermissionsPageComponent {
  private readonly permissionService = inject(PermissionService);

  protected readonly loading = signal(false);
  private readonly permissions = signal<Permission[]>([]);

  protected readonly skeletonCards: readonly number[] = [0, 1, 2, 3];
  protected readonly skeletonLines: readonly number[] = [0, 1, 2, 3];

  protected readonly totalPermissions = computed(() => this.permissions().length);

  protected readonly groups = computed<PermissionGroup[]>(() => {
    const byModule = new Map<string, Permission[]>();
    for (const permission of this.permissions()) {
      const list = byModule.get(permission.module) ?? [];
      list.push(permission);
      byModule.set(permission.module, list);
    }
    return Array.from(byModule.entries())
      .map(([module, permissions]) => ({ module, permissions }))
      .sort((a, b) => a.module.localeCompare(b.module));
  });

  constructor() {
    this.loading.set(true);
    this.permissionService.list().subscribe({
      next: (permissions) => {
        this.permissions.set(permissions);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected severityFor(action: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    switch (action) {
      case 'VIEW':
        return 'info';
      case 'CREATE':
        return 'success';
      case 'EDIT':
        return 'warn';
      case 'DELETE':
        return 'danger';
      default:
        return 'secondary';
    }
  }

  protected countLabel(value: number): string {
    return String(value);
  }
}
