import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ProgressBarModule } from 'primeng/progressbar';
import { ThemeStore } from './core/store/theme.store';
import { LoadingStore } from './core/store/loading.store';
import { LanguageService } from './core/services/language.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule, ConfirmDialogModule, ProgressBarModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly themeStore = inject(ThemeStore);
  protected readonly loadingStore = inject(LoadingStore);
  private readonly languageService = inject(LanguageService);

  constructor() {
    this.languageService.initialize();
  }
}
