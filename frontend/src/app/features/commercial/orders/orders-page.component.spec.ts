import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Order } from '../../../core/models/order.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { ContractService } from '../../../core/services/contract.service';
import { CustomerService } from '../../../core/services/customer.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { OrderService } from '../../../core/services/order.service';
import { OrdersPageComponent } from './orders-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function orderFixture(overrides: Partial<Order> = {}): Order {
  return {
    id: 'order-1',
    code: 'PED-001000',
    customer: null,
    proposal: null,
    opportunity: null,
    owner: null,
    status: 'PENDING',
    orderDate: '2026-02-01',
    deliveryDate: null,
    notes: null,
    totalAmount: 0,
    closedAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('OrdersPageComponent', () => {
  let fixture: ComponentFixture<OrdersPageComponent>;
  let component: OrdersPageComponent;
  let orderServiceStub: jasmine.SpyObj<OrderService>;
  let contractServiceStub: jasmine.SpyObj<ContractService>;

  beforeEach(async () => {
    localStorage.clear();

    orderServiceStub = jasmine.createSpyObj<OrderService>('OrderService', [
      'list',
      'create',
      'update',
      'changeStatus',
      'delete',
      'listItems',
      'createItem',
      'updateItem',
      'deleteItem',
      'createFromProposal'
    ]);
    orderServiceStub.list.and.returnValue(of(emptyPage));
    orderServiceStub.changeStatus.and.returnValue(of(orderFixture({ status: 'CONFIRMED' })));

    contractServiceStub = jasmine.createSpyObj<ContractService>('ContractService', ['createFromOrder']);

    await TestBed.configureTestingModule({
      imports: [OrdersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: OrderService, useValue: orderServiceStub },
        { provide: ContractService, useValue: contractServiceStub },
        { provide: CustomerService, useValue: { list: () => of(emptyPage), getById: () => of(null) } },
        { provide: OpportunityService, useValue: { list: () => of(emptyPage) } },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrdersPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no orders', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a customer before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.customerId.invalid).toBeTrue();
    expect(orderServiceStub.create).not.toHaveBeenCalled();
  });

  it('confirms an order through the status endpoint', () => {
    component['changeStatus'](orderFixture(), 'CONFIRMED');

    expect(orderServiceStub.changeStatus).toHaveBeenCalledWith('order-1', { status: 'CONFIRMED' });
  });

  it('opens the items dialog for the selected order', () => {
    const order = orderFixture();
    component['openItemsDialog'](order);

    expect(component['itemsDialogVisible']()).toBeTrue();
    expect(component['orderForItems']()).toEqual(order);
  });

  it('loads the linked values of an order when editing', () => {
    component['openEditDialog'](orderFixture({ notes: 'Entregar na filial' }));

    expect(component['form'].controls.notes.value).toBe('Entregar na filial');
    expect(component['form'].controls.orderDate.value).toBeInstanceOf(Date);
  });

  it('converts a delivered order into a contract', () => {
    contractServiceStub.createFromOrder.and.returnValue(
      of({
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
        updatedAt: '2026-01-01T00:00:00Z'
      })
    );

    component['convertToContract'](orderFixture({ status: 'DELIVERED' }));

    expect(contractServiceStub.createFromOrder).toHaveBeenCalledWith('order-1');
  });
});
