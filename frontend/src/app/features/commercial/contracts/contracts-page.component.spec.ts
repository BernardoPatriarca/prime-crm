import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Contract } from '../../../core/models/contract.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { ContractService } from '../../../core/services/contract.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { ContractsPageComponent } from './contracts-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function contractFixture(overrides: Partial<Contract> = {}): Contract {
  return {
    id: 'contract-1',
    code: 'CTR-001000',
    customer: null,
    order: null,
    opportunity: null,
    owner: null,
    billingCycle: null,
    status: 'DRAFT',
    startDate: '2026-02-01',
    endDate: null,
    autoRenew: false,
    recurringAmount: 0,
    notes: null,
    expired: false,
    terminatedAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('ContractsPageComponent', () => {
  let fixture: ComponentFixture<ContractsPageComponent>;
  let component: ContractsPageComponent;
  let contractServiceStub: jasmine.SpyObj<ContractService>;

  beforeEach(async () => {
    localStorage.clear();

    contractServiceStub = jasmine.createSpyObj<ContractService>('ContractService', [
      'list',
      'create',
      'update',
      'changeStatus',
      'delete',
      'createFromOrder'
    ]);
    contractServiceStub.list.and.returnValue(of(emptyPage));
    contractServiceStub.changeStatus.and.returnValue(of(contractFixture({ status: 'ACTIVE' })));

    await TestBed.configureTestingModule({
      imports: [ContractsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: ContractService, useValue: contractServiceStub },
        { provide: CustomerService, useValue: { list: () => of(emptyPage), getById: () => of(null) } },
        { provide: OpportunityService, useValue: { list: () => of(emptyPage) } },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContractsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no contracts', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a customer before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.customerId.invalid).toBeTrue();
    expect(contractServiceStub.create).not.toHaveBeenCalled();
  });

  it('activates a contract through the status endpoint', () => {
    component['changeStatus'](contractFixture(), 'ACTIVE');

    expect(contractServiceStub.changeStatus).toHaveBeenCalledWith('contract-1', { status: 'ACTIVE' });
  });

  it('loads the linked values of a contract when editing', () => {
    component['openEditDialog'](contractFixture({ notes: 'Renovacao anual', autoRenew: true }));

    expect(component['form'].controls.notes.value).toBe('Renovacao anual');
    expect(component['form'].controls.autoRenew.value).toBeTrue();
    expect(component['form'].controls.startDate.value).toBeInstanceOf(Date);
  });
});
