import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoadingStore } from '../store/loading.store';

export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiBaseUrl)) {
    return next(req);
  }

  const loadingStore = inject(LoadingStore);
  loadingStore.start();

  return next(req).pipe(finalize(() => loadingStore.stop()));
};
