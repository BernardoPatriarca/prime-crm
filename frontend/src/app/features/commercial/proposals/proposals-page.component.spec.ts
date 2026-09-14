import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Proposal } from '../../../core/models/proposal.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { CustomerService } from '../../../core/services/customer.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { ProposalService } from '../../../core/services/proposal.service';
import { ProposalsPageComponent } from './proposals-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function proposalFixture(overrides: Partial<Proposal> = {}): Proposal {
  return {
    id: 'proposal-1',
    code: 'PRO-001000',
    customer: null,
    contact: null,
    opportunity: null,
    owner: null,
    status: 'DRAFT',
    issueDate: '2026-02-01',
    validUntil: null,
    notes: null,
    totalAmount: 0,
    expired: false,
    decidedAt: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('ProposalsPageComponent', () => {
  let fixture: ComponentFixture<ProposalsPageComponent>;
  let component: ProposalsPageComponent;
  let proposalServiceStub: jasmine.SpyObj<ProposalService>;

  beforeEach(async () => {
    localStorage.clear();

    proposalServiceStub = jasmine.createSpyObj<ProposalService>('ProposalService', [
      'list',
      'create',
      'update',
      'changeStatus',
      'delete',
      'listItems',
      'createItem',
      'updateItem',
      'deleteItem'
    ]);
    proposalServiceStub.list.and.returnValue(of(emptyPage));
    proposalServiceStub.changeStatus.and.returnValue(of(proposalFixture({ status: 'SENT' })));

    await TestBed.configureTestingModule({
      imports: [ProposalsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: ProposalService, useValue: proposalServiceStub },
        { provide: CustomerService, useValue: { list: () => of(emptyPage), getById: () => of(null) } },
        { provide: OpportunityService, useValue: { list: () => of(emptyPage) } },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProposalsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no proposals', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a customer before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.customerId.invalid).toBeTrue();
    expect(proposalServiceStub.create).not.toHaveBeenCalled();
  });

  it('sends a proposal through the status endpoint', () => {
    component['changeStatus'](proposalFixture(), 'SENT');

    expect(proposalServiceStub.changeStatus).toHaveBeenCalledWith('proposal-1', { status: 'SENT' });
  });

  it('opens the items dialog for the selected proposal', () => {
    const proposal = proposalFixture();
    component['openItemsDialog'](proposal);

    expect(component['itemsDialogVisible']()).toBeTrue();
    expect(component['proposalForItems']()).toEqual(proposal);
  });

  it('loads the linked values of a proposal when editing', () => {
    component['openEditDialog'](proposalFixture({ notes: 'Cliente pediu desconto' }));

    expect(component['form'].controls.notes.value).toBe('Cliente pediu desconto');
    expect(component['form'].controls.issueDate.value).toBeInstanceOf(Date);
  });
});
