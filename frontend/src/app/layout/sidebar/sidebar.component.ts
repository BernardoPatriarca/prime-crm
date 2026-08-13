import { Component, computed, inject } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MenuItem, TooltipOptions } from 'primeng/api';
import { PanelMenuModule } from 'primeng/panelmenu';
import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
import { LayoutStore } from '../../core/store/layout.store';
import { SessionStore } from '../../core/store/session.store';

const GENERAL_REGISTRY_DOMAINS: { code: string; labelKey: string }[] = [
  { code: 'CLIENT_TYPE', labelKey: 'sidebar.settings.generalRegistries.clientType' },
  { code: 'PERSON_TYPE', labelKey: 'sidebar.settings.generalRegistries.personType' },
  { code: 'COMPANY_TYPE', labelKey: 'sidebar.settings.generalRegistries.companyType' },
  { code: 'MARKET_SEGMENT', labelKey: 'sidebar.settings.generalRegistries.marketSegment' },
  { code: 'ACTIVITY_BRANCH', labelKey: 'sidebar.settings.generalRegistries.activityBranch' },
  { code: 'LEAD_ORIGIN', labelKey: 'sidebar.settings.generalRegistries.leadOrigin' },
  { code: 'LOSS_REASON', labelKey: 'sidebar.settings.generalRegistries.lossReason' },
  { code: 'WIN_REASON', labelKey: 'sidebar.settings.generalRegistries.winReason' },
  { code: 'GENERIC_STATUS', labelKey: 'sidebar.settings.generalRegistries.status' },
  { code: 'PRIORITY', labelKey: 'sidebar.settings.generalRegistries.priority' },
  { code: 'TASK_TYPE', labelKey: 'sidebar.settings.generalRegistries.taskType' },
  { code: 'CATEGORY', labelKey: 'sidebar.settings.generalRegistries.category' },
  { code: 'TAG', labelKey: 'sidebar.settings.generalRegistries.tag' },
  { code: 'TEAM', labelKey: 'sidebar.settings.generalRegistries.team' },
  { code: 'POSITION', labelKey: 'sidebar.settings.generalRegistries.position' },
  { code: 'DEPARTMENT', labelKey: 'sidebar.settings.generalRegistries.department' }
];

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [NgTemplateOutlet, TranslatePipe, PanelMenuModule, ButtonModule, DrawerModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  private readonly translate = inject(TranslateService);
  protected readonly layoutStore = inject(LayoutStore);
  private readonly sessionStore = inject(SessionStore);

  private readonly railTooltipOptions: TooltipOptions = { tooltipPosition: 'right', showDelay: 120 };

  protected readonly menuItems = computed<MenuItem[]>(() => {
    this.translate.currentLang();
    const railMode = this.layoutStore.sidebarIconMode();
    const t = (key: string) => this.translate.instant(key);
    const hasPermission = (code: string) => this.sessionStore.hasPermission(code);

    const items: MenuItem[] = [
      {
        label: t('sidebar.dashboard'),
        icon: 'pi pi-home',
        routerLink: '/dashboard'
      }
    ];

    if (hasPermission('CLIENTES_VIEW')) {
      items.push({
        label: t('sidebar.modules.customers'),
        icon: 'pi pi-building',
        routerLink: '/clientes'
      });
    }

    if (hasPermission('OPORTUNIDADES_VIEW')) {
      items.push({
        label: t('sidebar.modules.opportunities'),
        icon: 'pi pi-chart-line',
        routerLink: '/oportunidades'
      });
    }

    if (hasPermission('TAREFAS_VIEW')) {
      items.push({
        label: t('sidebar.modules.tasks'),
        icon: 'pi pi-check-square',
        routerLink: '/tarefas'
      });
    }

    const commercialChildren: MenuItem[] = [];

    if (hasPermission('LEADS_VIEW')) {
      commercialChildren.push({
        label: t('sidebar.modules.leads'),
        icon: 'pi pi-bullseye',
        routerLink: '/leads'
      });
    }

    if (hasPermission('CONTATOS_VIEW')) {
      commercialChildren.push({
        label: t('sidebar.modules.contacts'),
        icon: 'pi pi-id-card',
        routerLink: '/contatos'
      });
    }

    if (hasPermission('CLIENTES_VIEW')) {
      commercialChildren.push({
        label: t('sidebar.modules.companies'),
        icon: 'pi pi-briefcase',
        routerLink: '/empresas'
      });
    }

    commercialChildren.push({
      label: t('sidebar.modules.finance'),
      icon: 'pi pi-wallet',
      disabled: true,
      badge: t('sidebar.comingSoon')
    });

    items.push({
      label: t('sidebar.modules.root'),
      icon: 'pi pi-th-large',
      items: commercialChildren
    });

    if (hasPermission('RELATORIOS_VIEW')) {
      items.push({
        label: t('sidebar.reports.root'),
        icon: 'pi pi-chart-bar',
        items: [
          {
            label: t('sidebar.reports.customers'),
            icon: 'pi pi-building',
            routerLink: '/relatorios/customers'
          },
          {
            label: t('sidebar.reports.opportunities'),
            icon: 'pi pi-chart-line',
            routerLink: '/relatorios/opportunities'
          },
          {
            label: t('sidebar.reports.tasks'),
            icon: 'pi pi-check-square',
            routerLink: '/relatorios/tasks'
          }
        ]
      });
    }

    const settingsChildren: MenuItem[] = [];

    if (hasPermission('DOMINIOS_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.generalRegistries.root'),
        icon: 'pi pi-database',
        items: GENERAL_REGISTRY_DOMAINS.map((domain) => ({
          label: t(domain.labelKey),
          routerLink: `/configuracoes/dominios/${domain.code}`
        }))
      });
    }

    if (hasPermission('PIPELINES_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.pipelines'),
        icon: 'pi pi-filter',
        routerLink: '/configuracoes/pipelines'
      });
    }

    if (hasPermission('CAMPOS_PERSONALIZADOS_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.customFields'),
        icon: 'pi pi-sliders-h',
        routerLink: '/configuracoes/campos-personalizados'
      });
    }

    if (hasPermission('TEMPLATES_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.templates'),
        icon: 'pi pi-file-edit',
        routerLink: '/configuracoes/templates'
      });
    }

    if (hasPermission('CONFIGURACOES_GERAIS_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.generalSettings'),
        icon: 'pi pi-cog',
        routerLink: '/configuracoes/gerais'
      });
    }

    if (hasPermission('FERIADOS_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.holidays'),
        icon: 'pi pi-calendar',
        routerLink: '/configuracoes/feriados'
      });
    }

    if (hasPermission('USUARIOS_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.users'),
        icon: 'pi pi-users',
        routerLink: '/configuracoes/usuarios'
      });
    }

    if (hasPermission('PERFIS_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.roles'),
        icon: 'pi pi-id-card',
        routerLink: '/configuracoes/perfis'
      });
    }

    if (hasPermission('PERMISSOES_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.permissions'),
        icon: 'pi pi-lock',
        routerLink: '/configuracoes/permissoes'
      });
    }

    if (hasPermission('AUDITORIA_VIEW')) {
      settingsChildren.push({
        label: t('sidebar.settings.audit'),
        icon: 'pi pi-history',
        routerLink: '/configuracoes/auditoria'
      });
    }

    if (settingsChildren.length > 0) {
      items.push({
        label: t('sidebar.settings.root'),
        icon: 'pi pi-wrench',
        items: settingsChildren
      });
    }

    if (!railMode) {
      return items;
    }

    return items.map((item) => ({
      ...item,
      tooltip: item.label,
      tooltipOptions: this.railTooltipOptions
    }));
  });
}
