import { inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

export const CREATE_QUERY_PARAM = 'novo';

export function openCreateDialogFromRoute(open: () => void): void {
  const route = inject(ActivatedRoute);
  const router = inject(Router);

  route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
    if (params.get(CREATE_QUERY_PARAM) !== '1') {
      return;
    }
    void router
      .navigate([], { relativeTo: route, queryParams: {}, replaceUrl: true })
      .then(() => open());
  });
}
