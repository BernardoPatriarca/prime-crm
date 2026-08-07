import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { SystemSetting } from '../../../core/models/system-setting.model';
import { SystemSettingService } from '../../../core/services/system-setting.service';
import { SessionStore } from '../../../core/store/session.store';

@Component({
  selector: 'app-system-settings-page',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, TableModule, ButtonModule, DialogModule, InputTextModule, TooltipModule],
  templateUrl: './system-settings-page.component.html',
  styleUrl: './system-settings-page.component.scss'
})
export class SystemSettingsPageComponent {
  private readonly systemSettingService = inject(SystemSettingService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly settings = signal<SystemSetting[]>([]);
  protected readonly loading = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editingSetting = signal<SystemSetting | null>(null);
  protected readonly saving = signal(false);

  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('CONFIGURACOES_GERAIS_EDIT'));

  protected readonly form = this.formBuilder.nonNullable.group({
    value: ['', [Validators.required]]
  });

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.systemSettingService.list().subscribe({
      next: (settings) => {
        this.settings.set(settings);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected openEditDialog(setting: SystemSetting): void {
    this.editingSetting.set(setting);
    this.form.reset({ value: setting.settingValue });
    this.dialogVisible.set(true);
  }

  protected closeDialog(): void {
    this.dialogVisible.set(false);
  }

  protected save(): void {
    const editing = this.editingSetting();
    if (!editing || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.systemSettingService.update(editing.settingKey, this.form.getRawValue().value).subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant('settingsPages.generalSettings.messages.updated')
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }
}
