import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MenuItem, MessageService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayBadgeModule } from 'primeng/overlaybadge';
import { PasswordModule } from 'primeng/password';
import { PopoverModule } from 'primeng/popover';
import { SelectModule } from 'primeng/select';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TieredMenuModule } from 'primeng/tieredmenu';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { AppNotification } from '../../core/models/notification.model';
import { SearchResult } from '../../core/models/search.model';
import { AuthService } from '../../core/services/auth.service';
import { GlobalSearchService } from '../../core/services/global-search.service';
import { AppLanguage, LanguageService, SUPPORTED_LANGUAGES } from '../../core/services/language.service';
import { NotificationService } from '../../core/services/notification.service';
import { LayoutStore } from '../../core/store/layout.store';
import { SessionStore } from '../../core/store/session.store';
import { ThemeStore } from '../../core/store/theme.store';
import { formatInstant } from '../../shared/utils/format.util';

const SEARCH_DEBOUNCE_MS = 300;
const MIN_SEARCH_LENGTH = 2;
const NOTIFICATION_REFRESH_MS = 120_000;

const NOTIFICATION_ICONS: Record<string, string> = {
  TASK_OVERDUE: 'pi pi-exclamation-circle',
  TASK_DUE_TODAY: 'pi pi-clock',
  OPPORTUNITY_CLOSE_DATE_PASSED: 'pi pi-chart-line',
  LEAD_WITHOUT_OWNER: 'pi pi-user-plus'
};

const SEARCH_ICONS: Record<string, string> = {
  CUSTOMER: 'pi pi-building',
  CONTACT: 'pi pi-id-card',
  LEAD: 'pi pi-bullseye',
  OPPORTUNITY: 'pi pi-chart-line',
  TASK: 'pi pi-check-square'
};

interface WorkspaceOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    TranslatePipe,
    AvatarModule,
    BadgeModule,
    ButtonModule,
    DialogModule,
    IconFieldModule,
    InputIconModule,
    InputTextModule,
    OverlayBadgeModule,
    PasswordModule,
    PopoverModule,
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
  private readonly notificationService = inject(NotificationService);
  private readonly globalSearchService = inject(GlobalSearchService);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly themeStore = inject(ThemeStore);
  protected readonly sessionStore = inject(SessionStore);
  protected readonly layoutStore = inject(LayoutStore);

  readonly scrolled = input(false);

  protected readonly userMenuOpen = signal(false);

  protected readonly notifications = signal<AppNotification[]>([]);
  protected readonly notificationTotal = signal(0);
  protected readonly notificationsLoading = signal(false);

  protected readonly searchTerm = signal('');
  protected readonly searchResults = signal<SearchResult[]>([]);
  protected readonly searching = signal(false);

  protected readonly passwordDialogVisible = signal(false);
  protected readonly savingPassword = signal(false);

  protected readonly workspaceOptions: WorkspaceOption[] = [
    { label: 'Workspace Principal', value: 'main' }
  ];
  protected readonly selectedWorkspace = 'main';

  protected readonly passwordForm = this.formBuilder.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirmPassword: ['', [Validators.required]]
  });

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

  protected readonly badgeValue = computed(() => {
    const total = this.notificationTotal();
    return total > 99 ? '99+' : String(total);
  });

  protected readonly newButtonItems = computed<MenuItem[]>(() => {
    this.translate.currentLang();
    const t = (key: string) => this.translate.instant(key);
    const hasPermission = (code: string) => this.sessionStore.hasPermission(code);

    return [
      {
        label: t('topbar.newButton.lead'),
        icon: 'pi pi-bullseye',
        disabled: !hasPermission('LEADS_CREATE'),
        command: () => this.openCreation('/leads')
      },
      {
        label: t('topbar.newButton.customer'),
        icon: 'pi pi-building',
        disabled: !hasPermission('CLIENTES_CREATE'),
        command: () => this.openCreation('/clientes')
      },
      {
        label: t('topbar.newButton.opportunity'),
        icon: 'pi pi-briefcase',
        disabled: !hasPermission('OPORTUNIDADES_CREATE'),
        command: () => this.openCreation('/oportunidades')
      },
      {
        label: t('topbar.newButton.task'),
        icon: 'pi pi-check-square',
        disabled: !hasPermission('TAREFAS_CREATE'),
        command: () => this.openCreation('/tarefas')
      }
    ];
  });

  protected readonly userMenuItems = computed<MenuItem[]>(() => {
    this.translate.currentLang();
    this.themeStore.mode();
    const t = (key: string) => this.translate.instant(key);
    const currentLanguage = this.languageService.getCurrentLanguage();

    return [
      {
        label: t('topbar.userMenu.changePassword'),
        icon: 'pi pi-key',
        command: () => this.openPasswordDialog()
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
        label: t('topbar.userMenu.logout'),
        icon: 'pi pi-sign-out',
        command: () => this.logout()
      }
    ];
  });

  private readonly searchInput = new Subject<string>();

  constructor() {
    this.searchInput
      .pipe(
        debounceTime(SEARCH_DEBOUNCE_MS),
        distinctUntilChanged(),
        switchMap((term) => this.globalSearchService.search(term)),
        takeUntilDestroyed()
      )
      .subscribe({
        next: (response) => {
          this.searchResults.set(response.results);
          this.searching.set(false);
        },
        error: () => this.searching.set(false)
      });

    this.loadNotifications();
    const timer = setInterval(() => this.loadNotifications(), NOTIFICATION_REFRESH_MS);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }

  protected loadNotifications(): void {
    if (!this.sessionStore.isAuthenticated()) {
      return;
    }
    this.notificationsLoading.set(true);
    this.notificationService.list().subscribe({
      next: (response) => {
        this.notifications.set(response.items);
        this.notificationTotal.set(response.total);
        this.notificationsLoading.set(false);
      },
      error: () => this.notificationsLoading.set(false)
    });
  }

  protected notificationIcon(notification: AppNotification): string {
    return NOTIFICATION_ICONS[notification.type] ?? 'pi pi-bell';
  }

  protected searchIcon(result: SearchResult): string {
    return SEARCH_ICONS[result.type] ?? 'pi pi-search';
  }

  protected formatDate(value: string | null): string {
    return formatInstant(value, '');
  }

  protected onSearchInput(value: string): void {
    this.searchTerm.set(value);
    const term = value.trim();
    if (term.length < MIN_SEARCH_LENGTH) {
      this.searchResults.set([]);
      this.searching.set(false);
      return;
    }
    this.searching.set(true);
    this.searchInput.next(term);
  }

  protected async goTo(link: string): Promise<void> {
    this.searchTerm.set('');
    this.searchResults.set([]);
    await this.router.navigateByUrl(link);
  }

  private async openCreation(route: string): Promise<void> {
    await this.router.navigate([route], { queryParams: { novo: 1 } });
  }

  protected async openDefaultCreation(): Promise<void> {
    const firstEnabled = this.newButtonItems().find((item) => !item.disabled);
    firstEnabled?.command?.({ originalEvent: undefined as never, item: firstEnabled });
  }

  protected openPasswordDialog(): void {
    this.passwordForm.reset({ currentPassword: '', newPassword: '', confirmPassword: '' });
    this.passwordDialogVisible.set(true);
  }

  protected closePasswordDialog(): void {
    this.passwordDialogVisible.set(false);
  }

  protected get passwordMismatch(): boolean {
    const { newPassword, confirmPassword } = this.passwordForm.getRawValue();
    return confirmPassword.length > 0 && newPassword !== confirmPassword;
  }

  protected savePassword(): void {
    if (this.passwordForm.invalid || this.passwordMismatch) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { currentPassword, newPassword } = this.passwordForm.getRawValue();
    this.savingPassword.set(true);
    this.authService.changeOwnPassword({ currentPassword, newPassword }).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.passwordDialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant('topbar.password.success')
        });
      },
      error: () => this.savingPassword.set(false)
    });
  }

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
