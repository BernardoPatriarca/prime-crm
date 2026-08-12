import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { DomainTypeService } from '../../../core/services/domain-type.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { DomainValuesPageComponent } from './domain-values-page.component';

describe('DomainValuesPageComponent', () => {
  let fixture: ComponentFixture<DomainValuesPageComponent>;
  let component: DomainValuesPageComponent;

  const domainTypeServiceStub: Partial<DomainTypeService> = {
    list: () =>
      of([{ id: '1', code: 'PRIORITY', label: 'Prioridade', supportsColor: true, supportsIcon: true, systemDefined: true }])
  };

  const domainValueServiceStub: Partial<DomainValueService> = {
    list: () => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [DomainValuesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: DomainTypeService, useValue: domainTypeServiceStub },
        { provide: DomainValueService, useValue: domainValueServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DomainValuesPageComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('tipo', 'PRIORITY');
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('carrega a listagem uma unica vez ao abrir a tela', async () => {
    let chamadasValores = 0;
    let chamadasTipos = 0;

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DomainValuesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        {
          provide: DomainTypeService,
          useValue: {
            list: () => {
              chamadasTipos += 1;
              return of([
                { id: '1', code: 'PRIORITY', label: 'Prioridade', supportsColor: true, supportsIcon: true, systemDefined: true }
              ]);
            }
          }
        },
        {
          provide: DomainValueService,
          useValue: {
            list: () => {
              chamadasValores += 1;
              return of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true });
            }
          }
        }
      ]
    }).compileComponents();

    const local = TestBed.createComponent(DomainValuesPageComponent);
    local.componentRef.setInput('tipo', 'PRIORITY');
    local.detectChanges();
    await local.whenStable();
    local.detectChanges();
    await local.whenStable();

    expect(chamadasValores).toBe(1);
    expect(chamadasTipos).toBe(1);
  });

  it('nao recarrega quando apenas o estado interno muda', async () => {
    let chamadasValores = 0;

    await TestBed.resetTestingModule().configureTestingModule({
      imports: [DomainValuesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: DomainTypeService, useValue: domainTypeServiceStub },
        {
          provide: DomainValueService,
          useValue: {
            list: () => {
              chamadasValores += 1;
              return of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true });
            }
          }
        }
      ]
    }).compileComponents();

    const local = TestBed.createComponent(DomainValuesPageComponent);
    local.componentRef.setInput('tipo', 'PRIORITY');
    local.detectChanges();
    await local.whenStable();

    const aposCarga = chamadasValores;

    for (let i = 0; i < 5; i += 1) {
      local.detectChanges();
      await local.whenStable();
    }

    expect(chamadasValores).toBe(aposCarga);
  });

  it('shows the empty state when there are no domain values', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a name before saving a new value', () => {
    component['openCreateDialog']();
    fixture.detectChanges();

    component['save']();

    expect(component['form'].controls.name.invalid).toBeTrue();
    expect(component['form'].controls.name.touched).toBeTrue();
  });
});
