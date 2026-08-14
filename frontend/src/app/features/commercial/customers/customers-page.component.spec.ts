import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { CustomersPageComponent } from './customers-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

describe('CustomersPageComponent', () => {
  let fixture: ComponentFixture<CustomersPageComponent>;
  let component: CustomersPageComponent;

  const customerServiceStub: Partial<CustomerService> = { list: () => of(emptyPage) };
  const domainValueServiceStub: Partial<DomainValueService> = { list: () => of(emptyPage) };
  const adminUserServiceStub: Partial<AdminUserService> = { list: () => of(emptyPage) };

  async function setup(routeData: Record<string, unknown> = {}): Promise<void> {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [CustomersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: CustomerService, useValue: customerServiceStub },
        { provide: DomainValueService, useValue: domainValueServiceStub },
        { provide: AdminUserService, useValue: adminUserServiceStub },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: routeData }, queryParamMap: of(convertToParamMap({})) }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CustomersPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('should create', async () => {
    await setup();
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no customers', async () => {
    await setup();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('renders the table exactly once and keeps it mounted while loading', async () => {
    await setup();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('[data-testid="generic-table"]').length).toBe(1);

    component['loading'].set(true);
    fixture.detectChanges();

    expect(compiled.querySelectorAll('[data-testid="generic-table"]').length).toBe(1);
  });

  it('requires a name before saving', async () => {
    await setup();
    component['openCreateDialog']();
    fixture.detectChanges();

    component['save']();

    expect(component['form'].controls.name.invalid).toBeTrue();
    expect(component['form'].controls.name.touched).toBeTrue();
  });

  it('rejects a CNPJ with an invalid check digit for a company', async () => {
    await setup();
    component['openCreateDialog']();
    component['form'].patchValue({ name: 'Empresa Teste', personType: 'JURIDICA', document: '11222333000180' });

    expect(component['form'].controls.document.hasError('cnpj')).toBeTrue();

    component['form'].controls.document.setValue('11222333000181');
    expect(component['form'].controls.document.valid).toBeTrue();
  });

  it('switches the document validator when the person type changes', async () => {
    await setup();
    component['openCreateDialog']();

    component['form'].controls.personType.setValue('FISICA');
    component['form'].controls.document.setValue('11144477735');
    expect(component['form'].controls.document.valid).toBeTrue();

    component['form'].controls.personType.setValue('JURIDICA');
    expect(component['form'].controls.document.hasError('cnpj')).toBeTrue();
  });

  it('jumps to the tab holding the first invalid field', async () => {
    await setup();
    component['openCreateDialog']();
    component['activeTab'].set('address');

    component['save']();

    expect(component['activeTab']()).toBe('general');
  });

  it('locks the person type when the route is the companies variant', async () => {
    await setup({ pageKey: 'companies', personType: 'JURIDICA' });

    expect(component['lockedPersonType']()).toBe('JURIDICA');
    expect(component['titleKey']()).toBe('commercialPages.companies.title');

    component['openCreateDialog']();
    expect(component['form'].controls.personType.value).toBe('JURIDICA');
  });

  it('uses the customers keys and no locked person type on the default route', async () => {
    await setup({ pageKey: 'customers' });

    expect(component['lockedPersonType']()).toBeNull();
    expect(component['titleKey']()).toBe('commercialPages.customers.title');
  });
});
