import { Signal, WritableSignal, computed, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';

export interface OptionsState<T> {
  items: WritableSignal<T[]>;
  failed: WritableSignal<boolean>;
  emptyMessage: Signal<string>;
  load(source: Observable<T[]>): void;
}

export function createOptionsState<T>(translate: TranslateService): OptionsState<T> {
  const items = signal<T[]>([]);
  const failed = signal(false);

  return {
    items,
    failed,
    emptyMessage: computed(() => {
      translate.currentLang();
      return translate.instant(failed() ? 'common.select.loadError' : 'common.select.empty');
    }),
    load(source: Observable<T[]>): void {
      source.subscribe({
        next: (loaded) => {
          items.set(loaded);
          failed.set(false);
        },
        error: () => {
          items.set([]);
          failed.set(true);
        }
      });
    }
  };
}
