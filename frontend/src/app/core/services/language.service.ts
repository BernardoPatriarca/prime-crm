import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

const LANGUAGE_KEY = 'prime-crm.language';

export const SUPPORTED_LANGUAGES = ['pt-BR', 'en', 'es'] as const;

export type AppLanguage = (typeof SUPPORTED_LANGUAGES)[number];

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);

  initialize(): void {
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
}
