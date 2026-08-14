import { Component } from '@angular/core';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { openCreateDialogFromRoute } from './creation-route.util';

@Component({ selector: 'app-creation-host', standalone: true, template: '' })
class CreationHostComponent {
  opened = 0;

  constructor() {
    openCreateDialogFromRoute(() => this.opened++);
  }
}

describe('openCreateDialogFromRoute', () => {
  const queryParamMap = new BehaviorSubject(convertToParamMap({}));
  let navigate: jasmine.Spy;

  function createHost(): CreationHostComponent {
    const fixture = TestBed.createComponent(CreationHostComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  beforeEach(() => {
    navigate = jasmine.createSpy('navigate').and.resolveTo(true);

    TestBed.configureTestingModule({
      imports: [CreationHostComponent],
      providers: [
        { provide: ActivatedRoute, useValue: { queryParamMap } },
        { provide: Router, useValue: { navigate } }
      ]
    });
  });

  it('does not open the dialog without the query param', () => {
    queryParamMap.next(convertToParamMap({}));

    expect(createHost().opened).toBe(0);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('opens the dialog when the route carries novo=1', fakeAsync(() => {
    queryParamMap.next(convertToParamMap({ novo: '1' }));

    const host = createHost();
    tick();

    expect(host.opened).toBe(1);
  }));

  it('clears the query param so the shortcut works twice in a row', fakeAsync(() => {
    queryParamMap.next(convertToParamMap({ novo: '1' }));

    createHost();
    tick();

    expect(navigate).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: {}, replaceUrl: true }));
  }));

  it('ignores any other value of the flag', fakeAsync(() => {
    queryParamMap.next(convertToParamMap({ novo: '0' }));

    const host = createHost();
    tick();

    expect(host.opened).toBe(0);
  }));
});
