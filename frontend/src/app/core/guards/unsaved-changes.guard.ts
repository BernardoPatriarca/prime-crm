import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';
import { ConfirmationService } from 'primeng/api';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (!component.hasUnsavedChanges()) {
    return true;
  }

  const confirmationService = inject(ConfirmationService);
  const translate = inject(TranslateService);

  return new Observable<boolean>((subscriber) => {
    confirmationService.confirm({
      header: translate.instant('common.unsavedChanges.title'),
      message: translate.instant('common.unsavedChanges.message'),
      acceptLabel: translate.instant('common.unsavedChanges.accept'),
      rejectLabel: translate.instant('common.unsavedChanges.reject'),
      accept: () => {
        subscriber.next(true);
        subscriber.complete();
      },
      reject: () => {
        subscriber.next(false);
        subscriber.complete();
      }
    });
  });
};
