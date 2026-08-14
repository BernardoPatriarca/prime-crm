import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';

const LANGUAGE_KEY = 'prime-crm.language';

export const SUPPORTED_LANGUAGES = ['pt-BR', 'en', 'es'] as const;

export type AppLanguage = (typeof SUPPORTED_LANGUAGES)[number];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly primeng = inject(PrimeNG);

  initialize(): void {
    this.translate.onLangChange.subscribe(() => this.applyComponentTranslations());
    this.translate.use(this.getStoredLanguage() ?? 'pt-BR');
  }

  getStoredLanguage(): AppLanguage | null {
    const raw = localStorage.getItem(LANGUAGE_KEY);
    return (SUPPORTED_LANGUAGES as readonly string[]).includes(raw ?? '') ? (raw as AppLanguage) : null;
  }

  getCurrentLanguage(): AppLanguage {
    return (this.translate.getCurrentLang() as AppLanguage) ?? 'pt-BR';
  }

  setLanguage(language: AppLanguage): void {
    localStorage.setItem(LANGUAGE_KEY, language);
    this.translate.use(language);
  }

  private applyComponentTranslations(): void {
    this.primeng.setTranslation({
      emptyMessage: this.translate.instant('common.select.empty'),
      emptyFilterMessage: this.translate.instant('common.select.emptyFilter'),
      emptySearchMessage: this.translate.instant('common.select.empty'),
      emptySelectionMessage: this.translate.instant('common.select.empty')
    });
  }
}
