import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { SessionStore } from '../store/session.store';

export const permissionGuard: CanActivateFn = (route) => {
  const sessionStore = inject(SessionStore);
  const router = inject(Router);
  const messageService = inject(MessageService);
  const translate = inject(TranslateService);

  const requiredPermission = route.data['permission'] as string | string[] | undefined;

  if (!requiredPermission || sessionStore.hasPermission(requiredPermission)) {
    return true;
  }

  messageService.add({
    severity: 'error',
    summary: translate.instant('common.accessDenied.title'),
    detail: translate.instant('common.accessDenied.message')
  });

  return router.createUrlTree(['/dashboard']);
};
