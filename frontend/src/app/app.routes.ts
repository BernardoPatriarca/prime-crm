import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
      },
      {
        path: 'clientes',
        canActivate: [permissionGuard],
        data: { permission: 'CLIENTES_VIEW', pageKey: 'customers' },
        loadComponent: () =>
          import('./features/commercial/customers/customers-page.component').then((m) => m.CustomersPageComponent)
      },
      {
        path: 'empresas',
        canActivate: [permissionGuard],
        data: { permission: 'CLIENTES_VIEW', pageKey: 'companies', personType: 'JURIDICA' },
        loadComponent: () =>
          import('./features/commercial/customers/customers-page.component').then((m) => m.CustomersPageComponent)
      },
      {
        path: 'contatos',
        canActivate: [permissionGuard],
        data: { permission: 'CONTATOS_VIEW' },
        loadComponent: () =>
          import('./features/commercial/contacts/contacts-page.component').then((m) => m.ContactsPageComponent)
      },
      {
        path: 'leads',
        canActivate: [permissionGuard],
        data: { permission: 'LEADS_VIEW' },
        loadComponent: () => import('./features/commercial/leads/leads-page.component').then((m) => m.LeadsPageComponent)
      },
      {
        path: 'oportunidades',
        canActivate: [permissionGuard],
        data: { permission: 'OPORTUNIDADES_VIEW' },
        loadComponent: () =>
          import('./features/commercial/opportunities/opportunities-page.component').then(
            (m) => m.OpportunitiesPageComponent
          )
      },
      {
        path: 'tarefas',
        canActivate: [permissionGuard],
        data: { permission: 'TAREFAS_VIEW' },
        loadComponent: () => import('./features/tasks/tasks-page.component').then((m) => m.TasksPageComponent)
      },
      {
        path: 'relatorios/:report',
        canActivate: [permissionGuard],
        data: { permission: 'RELATORIOS_VIEW' },
        loadComponent: () => import('./features/reports/reports-page.component').then((m) => m.ReportsPageComponent)
      },
      {
        path: 'configuracoes/auditoria',
        canActivate: [permissionGuard],
        data: { permission: 'AUDITORIA_VIEW' },
        loadComponent: () =>
          import('./features/settings/audit/audit-page.component').then((m) => m.AuditPageComponent)
      },
      {
        path: 'configuracoes/dominios/:tipo',
        canActivate: [permissionGuard],
        data: { permission: 'DOMINIOS_VIEW' },
        loadComponent: () =>
          import('./features/settings/domain-values/domain-values-page.component').then(
            (m) => m.DomainValuesPageComponent
          )
      },
      {
        path: 'configuracoes/pipelines',
        canActivate: [permissionGuard],
        data: { permission: 'PIPELINES_VIEW' },
        loadComponent: () =>
          import('./features/settings/pipelines/pipelines-page.component').then((m) => m.PipelinesPageComponent)
      },
      {
        path: 'configuracoes/campos-personalizados',
        canActivate: [permissionGuard],
        data: { permission: 'CAMPOS_PERSONALIZADOS_VIEW' },
        loadComponent: () =>
          import('./features/settings/custom-fields/custom-fields-page.component').then(
            (m) => m.CustomFieldsPageComponent
          )
      },
      {
        path: 'configuracoes/templates',
        canActivate: [permissionGuard],
        data: { permission: 'TEMPLATES_VIEW' },
        loadComponent: () =>
          import('./features/settings/templates/templates-page.component').then((m) => m.TemplatesPageComponent)
      },
      {
        path: 'configuracoes/gerais',
        canActivate: [permissionGuard],
        data: { permission: 'CONFIGURACOES_GERAIS_VIEW' },
        loadComponent: () =>
          import('./features/settings/system-settings/system-settings-page.component').then(
            (m) => m.SystemSettingsPageComponent
          )
      },
      {
        path: 'configuracoes/feriados',
        canActivate: [permissionGuard],
        data: { permission: 'FERIADOS_VIEW' },
        loadComponent: () =>
          import('./features/settings/holidays/holidays-page.component').then((m) => m.HolidaysPageComponent)
      },
      {
        path: 'configuracoes/usuarios',
        canActivate: [permissionGuard],
        data: { permission: 'USUARIOS_VIEW' },
        loadComponent: () => import('./features/settings/users/users-page.component').then((m) => m.UsersPageComponent)
      },
      {
        path: 'configuracoes/perfis',
        canActivate: [permissionGuard],
        data: { permission: 'PERFIS_VIEW' },
        loadComponent: () => import('./features/settings/roles/roles-page.component').then((m) => m.RolesPageComponent)
      },
      {
        path: 'configuracoes/permissoes',
        canActivate: [permissionGuard],
        data: { permission: 'PERMISSOES_VIEW' },
        loadComponent: () =>
          import('./features/settings/permissions/permissions-page.component').then((m) => m.PermissionsPageComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
