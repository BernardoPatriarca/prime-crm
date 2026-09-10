import { effect, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { LoadingStore } from './loading.store';

describe('LoadingStore', () => {
  let store: InstanceType<typeof LoadingStore>;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(LoadingStore);
  });

  it('starts hidden', () => {
    expect(store.isLoading()).toBeFalse();
  });

  it('shows while a request is active and hides once it is the last one to finish', () => {
    store.start();
    expect(store.isLoading()).toBeTrue();

    store.start();
    store.stop();
    expect(store.isLoading()).toBeTrue();

    store.stop();
    expect(store.isLoading()).toBeFalse();
  });

  it('never goes negative when stop() is called more times than start()', () => {
    store.stop();
    store.stop();

    expect(store.isLoading()).toBeFalse();
  });

  it(
    'regressao: start()/stop() nao podem virar dependencia reativa de um effect ambiente ' +
      '(travava a tela de Cadastros Gerais)',
    () => {
      // Ler o signal pela getter reativa dentro de start()/stop() os tornava dependencia de
      // qualquer effect que dispare HTTP, gerando loop infinito. patchState com updater evita isso.
      let runs = 0;
      const unrelatedTrigger = signal(0);

      TestBed.runInInjectionContext(() => {
        effect(() => {
          unrelatedTrigger();
          runs += 1;
          store.start();
          store.stop();
        });
      });

      TestBed.tick();
      expect(runs).toBe(1);

      for (let i = 0; i < 5; i += 1) {
        TestBed.tick();
      }

      expect(runs).toBe(1);
    }
  );
});
