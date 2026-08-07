import { Component, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuItem } from 'primeng/api';
import { OverlayBadgeModule } from 'primeng/overlaybadge';
import { SelectModule } from 'primeng/select';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TieredMenuModule } from 'primeng/tieredmenu';
import { AppLanguage, LanguageService, SUPPORTED_LANGUAGES } from '../../core/services/language.service';
import { LayoutStore } from '../../core/store/layout.store';
import { SessionStore } from '../../core/store/session.store';
import { ThemeStore } from '../../core/store/theme.store';

interface WorkspaceOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    FormsModule,
    TranslatePipe,
    AvatarModule,
    BadgeModule,
    ButtonModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    OverlayBadgeModule,
    SelectModule,
    SplitButtonModule,
    TieredMenuModule
  ],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss'
})
export class TopbarComponent {
  private readonly translate = inject(TranslateService);
  private readonly languageService = inject(LanguageService);
  private readonly router = inject(Router);
  protected readonly themeStore = inject(ThemeStore);
  protected readonly sessionStore = inject(SessionStore);
  protected readonly layoutStore = inject(LayoutStore);

  readonly scrolled = input(false);

  protected readonly userMenuOpen = signal(false);

  protected readonly workspaceOptions: WorkspaceOption[] = [
    { label: 'Workspace Principal', value: 'main' }
  ];
  protected readonly selectedWorkspace = 'main';

  protected readonly userInitials = computed(() => {
    const name = this.sessionStore.user()?.name?.trim();
    if (!name) {
      return '?';
    }
    const parts = name.split(/\s+/).filter((part) => part.length > 0);
    const first = parts[0]?.charAt(0) ?? '';
    const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
    return (first + last).toUpperCase();
  });

  protected readonly newButtonItems = computed<MenuItem[]>(() => {
    this.translate.currentLang();
    const t = (key: string) => this.translate.instant(key);
    return [
      { label: t('topbar.newButton.lead'), icon: 'pi pi-bullseye', disabled: true },
      { label: t('topbar.newButton.customer'), icon: 'pi pi-building', disabled: true },
      { label: t('topbar.newButton.opportunity'), icon: 'pi pi-briefcase', disabled: true },
      { label: t('topbar.newButton.task'), icon: 'pi pi-check-square', disabled: true }
    ];
  });

  protected readonly userMenuItems = computed<MenuItem[]>(() => {
    this.translate.currentLang();
    this.themeStore.mode();
    const t = (key: string) => this.translate.instant(key);
    const currentLanguage = this.languageService.getCurrentLanguage();

    return [
      {
        label: t('topbar.userMenu.editProfile'),
        icon: 'pi pi-user-edit',
        disabled: true
      },
      {
        label: t('topbar.userMenu.changePassword'),
        icon: 'pi pi-key',
        disabled: true
      },
      {
        label: t('topbar.userMenu.preferences'),
        icon: 'pi pi-sliders-h',
        disabled: true
      },
      { separator: true },
      {
        label: this.themeStore.isDark() ? t('topbar.userMenu.theme.light') : t('topbar.userMenu.theme.dark'),
        icon: this.themeStore.isDark() ? 'pi pi-sun' : 'pi pi-moon',
        command: () => this.themeStore.toggle()
      },
      {
        label: t('topbar.userMenu.language.label'),
        icon: 'pi pi-globe',
        items: SUPPORTED_LANGUAGES.map((language) => ({
          label: this.languageLabel(language, t),
          icon: language === currentLanguage ? 'pi pi-check' : undefined,
          command: () => this.languageService.setLanguage(language)
        }))
      },
      { separator: true },
      {
        label: t('topbar.userMenu.helpCenter'),
        icon: 'pi pi-question-circle',
        disabled: true
      },
      {
        label: t('topbar.userMenu.logout'),
        icon: 'pi pi-sign-out',
        command: () => this.logout()
      }
    ];
  });

  private languageLabel(language: AppLanguage, t: (key: string) => string): string {
    const keys: Record<AppLanguage, string> = {
      'pt-BR': 'topbar.userMenu.language.ptBR',
      en: 'topbar.userMenu.language.en',
      es: 'topbar.userMenu.language.es'
    };
    return t(keys[language]);
  }

  protected async logout(): Promise<void> {
    await this.sessionStore.logout();
    await this.router.navigateByUrl('/login');
  }
}
