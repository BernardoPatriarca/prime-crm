import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { PermissionService } from '../../../core/services/permission.service';
import { PermissionsPageComponent } from './permissions-page.component';

describe('PermissionsPageComponent', () => {
  let fixture: ComponentFixture<PermissionsPageComponent>;

  it('shows the empty state when there are no permissions', async () => {
    await TestBed.configureTestingModule({
      imports: [PermissionsPageComponent],
      providers: [
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        { provide: PermissionService, useValue: { list: () => of([]) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PermissionsPageComponent);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="permissions-empty"]')).toBeTruthy();
  });

  it('groups permissions by module', async () => {
    await TestBed.configureTestingModule({
      imports: [PermissionsPageComponent],
      providers: [
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        {
          provide: PermissionService,
          useValue: {
            list: () =>
              of([
                { id: '1', code: 'USUARIOS_VIEW', module: 'USUARIOS', action: 'VIEW', description: null },
                { id: '2', code: 'PERFIS_VIEW', module: 'PERFIS', action: 'VIEW', description: null }
              ])
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PermissionsPageComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance['groups']().length).toBe(2);
  });
});
