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
      // O loadingInterceptor chama loadingStore.start()/.stop() de forma SINCRONA no meio de
      // toda chamada HTTP disparada pela aplicacao. Se start()/stop() lessem o valor atual do
      // signal atraves da getter reativa (`store.activeRequests()`) para calcular o incremento —
      // como faziam antes desta correcao — qualquer effect() que disparasse uma chamada HTTP
      // sincronamente (ex.: um efeito de tela que chama um service.list().subscribe(...) sem
      // envolver a chamada em untracked()) passaria a "depender" desse signal. Como o proprio
      // start() ESCREVE nesse signal logo em seguida, o efeito seria marcado como sujo e
      // reagendado — chamando load() de novo, disparando outra requisicao, chamando start() de
      // novo, e assim indefinidamente. Foi exatamente isso que travou a aba inteira ao abrir
      // "Configuracoes > Cadastros Gerais > Tipo de Cliente".
      //
      // A correcao usa a forma de updater function do patchState (que recebe um snapshot do
      // estado, nao uma leitura reativa do signal), entao start()/stop() nunca mais podem ser
      // capturados como dependencia de um effect ambiente, nao importa de onde sejam chamados.
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
