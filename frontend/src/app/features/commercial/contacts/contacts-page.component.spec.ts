import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { ContactService } from '../../../core/services/contact.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { ContactsPageComponent } from './contacts-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

describe('ContactsPageComponent', () => {
  let fixture: ComponentFixture<ContactsPageComponent>;
  let component: ContactsPageComponent;

  const contactServiceStub: Partial<ContactService> = { list: () => of(emptyPage) };
  const customerServiceStub: Partial<CustomerService> = { list: () => of(emptyPage) };
  const domainValueServiceStub: Partial<DomainValueService> = { list: () => of(emptyPage) };

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [ContactsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: ContactService, useValue: contactServiceStub },
        { provide: CustomerService, useValue: customerServiceStub },
        { provide: DomainValueService, useValue: domainValueServiceStub }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContactsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no contacts', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('keeps a single table mounted while loading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    component['loading'].set(true);
    fixture.detectChanges();

    expect(compiled.querySelectorAll('[data-testid="generic-table"]').length).toBe(1);
  });

  it('requires a customer and a name before saving', () => {
    component['openCreateDialog']();
    fixture.detectChanges();

    component['save']();

    expect(component['form'].controls.customerId.invalid).toBeTrue();
    expect(component['form'].controls.name.invalid).toBeTrue();
  });

  it('prefills the customer from the active filter when creating', () => {
    component['customerFilter'].set('customer-1');
    component['openCreateDialog']();

    expect(component['form'].controls.customerId.value).toBe('customer-1');
  });

  it('keeps the linked customer selectable when editing a contact outside the loaded options', () => {
    component['openEditDialog']({
      id: 'contact-1',
      customer: { id: 'customer-9', code: 'CLI-000009', name: 'Cliente Distante' },
      name: 'Maria',
      positionTitle: 'Compras',
      department: null,
      email: null,
      phone: null,
      mobile: null,
      birthDate: null,
      linkedin: null,
      primaryContact: true,
      decisionMaker: false,
      notes: null,
      active: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z'
    });

    expect(component['customerOptions']().some((option) => option.id === 'customer-9')).toBeTrue();
    expect(component['form'].controls.customerId.value).toBe('customer-9');
  });
});
