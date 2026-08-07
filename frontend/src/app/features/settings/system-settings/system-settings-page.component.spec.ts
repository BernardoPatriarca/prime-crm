import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { SystemSettingService } from '../../../core/services/system-setting.service';
import { SystemSettingsPageComponent } from './system-settings-page.component';

describe('SystemSettingsPageComponent', () => {
  let fixture: ComponentFixture<SystemSettingsPageComponent>;
  let component: SystemSettingsPageComponent;

  const systemSettingServiceStub: Partial<SystemSettingService> = {
    list: () => of([])
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [SystemSettingsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        { provide: SystemSettingService, useValue: systemSettingServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(SystemSettingsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no settings', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="system-settings-empty"]')).toBeTruthy();
  });

  it('requires a value before saving', () => {
    component['openEditDialog']({ id: '1', settingKey: 'SLA_DEFAULT_DAYS', settingValue: '5', description: null });
    component['form'].controls.value.setValue('');

    component['save']();

    expect(component['form'].controls.value.invalid).toBeTrue();
    expect(component['form'].controls.value.touched).toBeTrue();
  });
});
