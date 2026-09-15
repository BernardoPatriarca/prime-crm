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
        path: 'produtividade/dashboard',
        canActivate: [permissionGuard],
        data: { permission: ['TAREFAS_VIEW', 'AGENDA_VIEW'] },
        loadComponent: () =>
          import('./features/productivity/dashboard/productivity-dashboard.component').then(
            (m) => m.ProductivityDashboardComponent
          )
      },
      {
        path: 'tarefas',
        canActivate: [permissionGuard],
        data: { permission: 'TAREFAS_VIEW' },
        loadComponent: () => import('./features/tasks/tasks-page.component').then((m) => m.TasksPageComponent)
      },
      {
        path: 'agenda',
        canActivate: [permissionGuard],
        data: { permission: 'AGENDA_VIEW' },
        loadComponent: () => import('./features/agenda/agenda-page.component').then((m) => m.AgendaPageComponent)
      },
      {
        path: 'produtos',
        canActivate: [permissionGuard],
        data: { permission: 'PRODUTOS_VIEW' },
        loadComponent: () =>
          import('./features/commercial/products/products-page.component').then((m) => m.ProductsPageComponent)
      },
      {
        path: 'comercial/dashboard',
        canActivate: [permissionGuard],
        data: { permission: ['PROPOSTAS_VIEW', 'PEDIDOS_VIEW', 'CONTRATOS_VIEW'] },
        loadComponent: () =>
          import('./features/commercial/dashboard/commercial-dashboard.component').then(
            (m) => m.CommercialDashboardComponent
          )
      },
      {
        path: 'propostas',
        canActivate: [permissionGuard],
        data: { permission: 'PROPOSTAS_VIEW' },
        loadComponent: () =>
          import('./features/commercial/proposals/proposals-page.component').then((m) => m.ProposalsPageComponent)
      },
      {
        path: 'pedidos',
        canActivate: [permissionGuard],
        data: { permission: 'PEDIDOS_VIEW' },
        loadComponent: () =>
          import('./features/commercial/orders/orders-page.component').then((m) => m.OrdersPageComponent)
      },
      {
        path: 'contratos',
        canActivate: [permissionGuard],
        data: { permission: 'CONTRATOS_VIEW' },
        loadComponent: () =>
          import('./features/commercial/contracts/contracts-page.component').then((m) => m.ContractsPageComponent)
      },
      {
        path: 'financeiro/dashboard',
        canActivate: [permissionGuard],
        data: { permission: 'FINANCEIRO_VIEW' },
        loadComponent: () =>
          import('./features/finance/dashboard/finance-dashboard.component').then(
            (m) => m.FinanceDashboardComponent
          )
      },
      {
        path: 'financeiro/contas-a-receber',
        canActivate: [permissionGuard],
        data: { permission: 'FINANCEIRO_VIEW' },
        loadComponent: () =>
          import('./features/finance/receivables/receivables-page.component').then((m) => m.ReceivablesPageComponent)
      },
      {
        path: 'financeiro/contas-a-pagar',
        canActivate: [permissionGuard],
        data: { permission: 'FINANCEIRO_VIEW' },
        loadComponent: () =>
          import('./features/finance/payables/payables-page.component').then((m) => m.PayablesPageComponent)
      },
      {
        path: 'relatorios/:report',
        canActivate: [permissionGuard],
        data: { permission: 'RELATORIOS_VIEW' },
        loadComponent: () => import('./features/reports/reports-page.component').then((m) => m.ReportsPageComponent)
      },
      {
        path: 'metas-comerciais',
        canActivate: [permissionGuard],
        data: { permission: 'METAS_VIEW' },
        loadComponent: () =>
          import('./features/sales-goals/sales-goals-page.component').then((m) => m.SalesGoalsPageComponent)
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
