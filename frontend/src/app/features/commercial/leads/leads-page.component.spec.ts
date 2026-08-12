import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Lead } from '../../../core/models/lead.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { LeadService } from '../../../core/services/lead.service';
import { PipelineService } from '../../../core/services/pipeline.service';
import { LeadsPageComponent } from './leads-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function buildLead(overrides: Partial<Lead> = {}): Lead {
  return {
    id: 'lead-1',
    code: 'LEAD-000001',
    name: 'Lead Teste',
    companyName: 'Empresa Teste',
    contactName: null,
    email: null,
    phone: null,
    mobile: null,
    origin: null,
    status: null,
    priority: null,
    campaign: null,
    owner: null,
    pipeline: null,
    stage: null,
    probability: null,
    estimatedValue: 2500,
    expectedCloseDate: '2026-09-30',
    qualificationScore: null,
    convertedCustomer: null,
    convertedAt: null,
    notes: null,
    active: true,
    tags: [],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('LeadsPageComponent', () => {
  let fixture: ComponentFixture<LeadsPageComponent>;
  let component: LeadsPageComponent;
  let leadServiceStub: Partial<LeadService>;

  beforeEach(async () => {
    localStorage.clear();

    leadServiceStub = {
      list: () => of(emptyPage),
      convert: jasmine
        .createSpy('convert')
        .and.returnValue(of({ lead: buildLead(), customer: { name: 'Cliente Novo' }, opportunity: null }))
    };

    await TestBed.configureTestingModule({
      imports: [LeadsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: LeadService, useValue: leadServiceStub },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } },
        { provide: PipelineService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LeadsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no leads', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('keeps a single table mounted while loading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    component['loading'].set(true);
    fixture.detectChanges();

    expect(compiled.querySelectorAll('[data-testid="generic-table"]').length).toBe(1);
  });

  it('requires a name before saving', () => {
    component['openCreateDialog']();
    fixture.detectChanges();

    component['save']();

    expect(component['form'].controls.name.invalid).toBeTrue();
    expect(component['form'].controls.name.touched).toBeTrue();
  });

  it('prefills the convert dialog from the lead', () => {
    component['openConvertDialog'](buildLead());

    expect(component['convertDialogVisible']()).toBeTrue();
    expect(component['convertForm'].controls.createOpportunity.value).toBeTrue();
    expect(component['convertForm'].controls.opportunityTitle.value).toBe('Empresa Teste');
    expect(component['convertForm'].controls.amount.value).toBe(2500);
  });

  it('blocks the conversion when an opportunity is requested without a pipeline', () => {
    component['openConvertDialog'](buildLead());
    component['convertForm'].controls.pipelineId.setValue(null);

    component['submitConvert']();

    expect(leadServiceStub.convert).not.toHaveBeenCalled();
    expect(component['convertForm'].controls.pipelineId.hasError('required')).toBeTrue();
  });

  it('converts without a pipeline when no opportunity is requested', () => {
    component['openConvertDialog'](buildLead());
    component['convertForm'].controls.createOpportunity.setValue(false);
    component['convertForm'].controls.pipelineId.setValue(null);

    component['submitConvert']();

    expect(leadServiceStub.convert).toHaveBeenCalled();
    const request = (leadServiceStub.convert as jasmine.Spy).calls.mostRecent().args[1];
    expect(request.createOpportunity).toBeFalse();
    expect(request.pipelineId).toBeNull();
  });

  it('sends the pipeline and opportunity details when requested', () => {
    component['openConvertDialog'](buildLead());
    component['convertForm'].controls.pipelineId.setValue('pipeline-1');
    component['convertForm'].controls.stageId.setValue('stage-1');

    component['submitConvert']();

    const request = (leadServiceStub.convert as jasmine.Spy).calls.mostRecent().args[1];
    expect(request.createOpportunity).toBeTrue();
    expect(request.pipelineId).toBe('pipeline-1');
    expect(request.stageId).toBe('stage-1');
    expect(request.expectedCloseDate).toBe('2026-09-30');
  });

  it('formats currency and dates for the listing', () => {
    expect(component['formatDate']('2026-09-30')).toBe('30/09/2026');
    expect(component['formatDate'](null)).toBe('-');
    expect(component['formatCurrency'](null)).toBe('-');
    expect(component['formatCurrency'](2500)).toContain('2.500');
  });
});
