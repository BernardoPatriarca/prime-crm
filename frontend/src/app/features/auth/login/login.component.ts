import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MessageService } from 'primeng/api';
import { PasswordModule } from 'primeng/password';
import { ApiErrorResponse } from '../../../core/models/api-error.model';
import { SessionStore } from '../../../core/store/session.store';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    ButtonModule,
    CardModule,
    InputTextModule,
    MessageModule,
    PasswordModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly sessionStore = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly translate = inject(TranslateService);

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    usernameOrEmail: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  protected async submit(): Promise<void> {
    this.errorMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    try {
      await this.sessionStore.login(this.form.getRawValue());
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('auth.login.success')
      });
      await this.router.navigateByUrl('/dashboard');
    } catch (error: unknown) {
      this.errorMessage.set(this.resolveErrorMessage(error));
      this.form.controls.password.reset();
    } finally {
      this.submitting.set(false);
    }
  }

  private resolveErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return this.translate.instant('auth.login.genericError');
    }

    if (error.status === 0) {
      return this.translate.instant('auth.login.connectionError');
    }

    const errorCode = (error.error as ApiErrorResponse | undefined)?.errorCode;

    if (errorCode === 'USER_NOT_ACTIVE') {
      return this.translate.instant('auth.login.userNotActive');
    }

    if (errorCode === 'INVALID_CREDENTIALS' || error.status === 401) {
      return this.translate.instant('auth.login.invalidCredentials');
    }

    if (error.status >= 500) {
      return this.translate.instant('auth.login.serverError');
    }

    return this.translate.instant('auth.login.genericError');
  }
}
