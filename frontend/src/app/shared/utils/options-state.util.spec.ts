import { TestBed } from '@angular/core/testing';
import { TranslateService, provideTranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { createOptionsState } from './options-state.util';

describe('createOptionsState', () => {
  let translate: TranslateService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' })]
    });
    translate = TestBed.inject(TranslateService);
  });

  it('starts empty, without failure', () => {
    const state = createOptionsState<string>(translate);

    expect(state.items()).toEqual([]);
    expect(state.failed()).toBeFalse();
    expect(state.emptyMessage()).toContain('common.select.empty');
  });

  it('keeps the loaded options and clears the failure flag', () => {
    const state = createOptionsState<string>(translate);

    state.load(of(['a', 'b']));

    expect(state.items()).toEqual(['a', 'b']);
    expect(state.failed()).toBeFalse();
    expect(state.emptyMessage()).toContain('common.select.empty');
  });

  it('switches the message to the load error when the request fails', () => {
    const state = createOptionsState<string>(translate);

    state.load(throwError(() => new Error('offline')));

    expect(state.items()).toEqual([]);
    expect(state.failed()).toBeTrue();
    expect(state.emptyMessage()).toContain('common.select.loadError');
  });

  it('recovers the empty message after a successful reload', () => {
    const state = createOptionsState<string>(translate);

    state.load(throwError(() => new Error('offline')));
    state.load(of(['a']));

    expect(state.failed()).toBeFalse();
    expect(state.emptyMessage()).toContain('common.select.empty');
  });
});
