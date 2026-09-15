import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Payable } from '../../../core/models/payable.model';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { PayableService } from '../../../core/services/payable.service';
import { PayablesPageComponent } from './payables-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function payableFixture(overrides: Partial<Payable> = {}): Payable {
  return {
    id: 'payable-1',
    code: 'PAG-001000',
    supplier: null,
    category: null,
    description: 'Aluguel',
    dueDate: '2026-02-01',
    amount: 300,
    paidAmount: 0,
    remainingAmount: 300,
    paidAt: null,
    paymentMethod: null,
    status: 'PENDING',
    overdue: false,
    notes: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('PayablesPageComponent', () => {
  let fixture: ComponentFixture<PayablesPageComponent>;
  let component: PayablesPageComponent;
  let payableServiceStub: jasmine.SpyObj<PayableService>;

  beforeEach(async () => {
    localStorage.clear();

    payableServiceStub = jasmine.createSpyObj<PayableService>('PayableService', [
      'list',
      'create',
      'update',
      'delete',
      'pay'
    ]);
    payableServiceStub.list.and.returnValue(of(emptyPage));
    payableServiceStub.pay.and.returnValue(of(payableFixture({ status: 'PAID' })));

    await TestBed.configureTestingModule({
      imports: [PayablesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: PayableService, useValue: payableServiceStub },
        { provide: CustomerService, useValue: { list: () => of(emptyPage), getById: () => of(null) } },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PayablesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no payables', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a supplier and description before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.supplierId.invalid).toBeTrue();
    expect(component['form'].controls.description.invalid).toBeTrue();
    expect(payableServiceStub.create).not.toHaveBeenCalled();
  });

  it('opens the pay dialog with the payable remaining balance', () => {
    const payable = payableFixture({ remainingAmount: 150 });
    component['openPayDialog'](payable);

    expect(component['payDialogVisible']()).toBeTrue();
    expect(component['payingPayable']()?.remainingAmount).toBe(150);
  });

  it('registers a payment without an explicit amount to settle the full balance', () => {
    component['openPayDialog'](payableFixture());
    component['confirmPay']();

    expect(payableServiceStub.pay).toHaveBeenCalledWith('payable-1', { amount: null, paymentMethodId: null });
  });

  it('loads the linked values of a payable when editing', () => {
    component['openEditDialog'](payableFixture({ description: 'Conta de luz' }));

    expect(component['form'].controls.description.value).toBe('Conta de luz');
    expect(component['form'].controls.dueDate.value).toBeInstanceOf(Date);
  });
});
