import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { HolidayService } from '../../../core/services/holiday.service';
import { HolidaysPageComponent } from './holidays-page.component';

describe('HolidaysPageComponent', () => {
  let fixture: ComponentFixture<HolidaysPageComponent>;
  let component: HolidaysPageComponent;

  const holidayServiceStub: Partial<HolidayService> = {
    list: () => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true })
  };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [HolidaysPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: HolidayService, useValue: holidayServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HolidaysPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no holidays', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a name before saving a new holiday', () => {
    component['openCreateDialog']();
    fixture.detectChanges();

    component['save']();

    expect(component['form'].controls.name.invalid).toBeTrue();
    expect(component['form'].controls.name.touched).toBeTrue();
  });
});
