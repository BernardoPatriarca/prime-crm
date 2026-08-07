import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { CustomFieldService } from '../../../core/services/custom-field.service';
import { CustomFieldsPageComponent } from './custom-fields-page.component';

describe('CustomFieldsPageComponent', () => {
  let fixture: ComponentFixture<CustomFieldsPageComponent>;
  let component: CustomFieldsPageComponent;

  const customFieldServiceStub: Partial<CustomFieldService> = {
    list: () => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [CustomFieldsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: CustomFieldService, useValue: customFieldServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomFieldsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('hides the options editor for TEXT fields and shows it for SELECT fields', () => {
    component['openCreateDialog']();
    fixture.detectChanges();
    expect(component['showOptionsEditor']()).toBeFalse();

    component['form'].controls.fieldType.setValue('SELECT');
    expect(component['showOptionsEditor']()).toBeTrue();
  });

  it('requires the target entity, field key and label before saving', () => {
    component['openCreateDialog']();
    component['form'].reset({
      targetEntity: '',
      fieldKey: '',
      label: '',
      fieldType: 'TEXT',
      required: false,
      displayOrder: 0,
      active: true
    });

    component['save']();

    expect(component['form'].controls.targetEntity.invalid).toBeTrue();
    expect(component['form'].controls.fieldKey.invalid).toBeTrue();
    expect(component['form'].controls.label.invalid).toBeTrue();
  });
});
