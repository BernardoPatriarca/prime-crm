import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Receivable } from '../../../core/models/receivable.model';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { OrderService } from '../../../core/services/order.service';
import { ReceivableService } from '../../../core/services/receivable.service';
import { ReceivablesPageComponent } from './receivables-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function receivableFixture(overrides: Partial<Receivable> = {}): Receivable {
  return {
    id: 'receivable-1',
    code: 'REC-001000',
    customer: null,
    order: null,
    contract: null,
    description: null,
    installmentNumber: 1,
    totalInstallments: 1,
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

describe('ReceivablesPageComponent', () => {
  let fixture: ComponentFixture<ReceivablesPageComponent>;
  let component: ReceivablesPageComponent;
  let receivableServiceStub: jasmine.SpyObj<ReceivableService>;

  beforeEach(async () => {
    localStorage.clear();

    receivableServiceStub = jasmine.createSpyObj<ReceivableService>('ReceivableService', [
      'list',
      'create',
      'update',
      'delete',
      'pay',
      'generateFromOrder'
    ]);
    receivableServiceStub.list.and.returnValue(of(emptyPage));
    receivableServiceStub.pay.and.returnValue(of(receivableFixture({ status: 'PAID' })));
    receivableServiceStub.generateFromOrder.and.returnValue(of([receivableFixture()]));

    await TestBed.configureTestingModule({
      imports: [ReceivablesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: ReceivableService, useValue: receivableServiceStub },
        { provide: CustomerService, useValue: { list: () => of(emptyPage), getById: () => of(null) } },
        { provide: OrderService, useValue: { list: () => of(emptyPage) } },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReceivablesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no receivables', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a customer before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.customerId.invalid).toBeTrue();
    expect(receivableServiceStub.create).not.toHaveBeenCalled();
  });

  it('opens the pay dialog with the receivable remaining balance', () => {
    const receivable = receivableFixture({ remainingAmount: 150 });
    component['openPayDialog'](receivable);

    expect(component['payDialogVisible']()).toBeTrue();
    expect(component['payingReceivable']()?.remainingAmount).toBe(150);
  });

  it('registers a payment without an explicit amount to settle the full balance', () => {
    component['openPayDialog'](receivableFixture());
    component['confirmPay']();

    expect(receivableServiceStub.pay).toHaveBeenCalledWith('receivable-1', { amount: null, paymentMethodId: null });
  });

  it('requires an order before generating installments', () => {
    component['openGenerateDialog']();
    component['confirmGenerate']();

    expect(component['generateForm'].controls.orderId.invalid).toBeTrue();
    expect(receivableServiceStub.generateFromOrder).not.toHaveBeenCalled();
  });

  it('loads the linked values of a receivable when editing', () => {
    component['openEditDialog'](receivableFixture({ description: 'Entrada' }));

    expect(component['form'].controls.description.value).toBe('Entrada');
    expect(component['form'].controls.dueDate.value).toBeInstanceOf(Date);
  });
});
