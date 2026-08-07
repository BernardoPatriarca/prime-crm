import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { TemplateService } from '../../../core/services/template.service';
import { TemplatesPageComponent } from './templates-page.component';

describe('TemplatesPageComponent', () => {
  let fixture: ComponentFixture<TemplatesPageComponent>;
  let component: TemplatesPageComponent;

  const templateServiceStub: Partial<TemplateService> = {
    list: () => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [TemplatesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: TemplateService, useValue: templateServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TemplatesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('requires a subject when the type is EMAIL', () => {
    component['openCreateDialog']();
    component['form'].setValue({ type: 'EMAIL', name: 'Boas-vindas', subject: '', content: 'Ola', active: true });

    expect(component['form'].invalid).toBeTrue();
    expect(component['form'].errors?.['subjectRequiredForEmail']).toBeTrue();
  });

  it('does not require a subject when the type is WHATSAPP', () => {
    component['openCreateDialog']();
    component['form'].setValue({ type: 'WHATSAPP', name: 'Cobranca', subject: '', content: 'Ola', active: true });

    expect(component['form'].errors?.['subjectRequiredForEmail']).toBeFalsy();
  });
});
