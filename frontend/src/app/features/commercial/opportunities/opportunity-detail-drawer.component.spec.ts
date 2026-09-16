import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { Opportunity, OpportunityStageHistory } from '../../../core/models/opportunity.model';
import { OpportunityDetailDrawerComponent } from './opportunity-detail-drawer.component';

function buildOpportunity(overrides: Partial<Opportunity> = {}): Opportunity {
  return {
    id: 'opp-1',
    code: 'OPO-000001',
    title: 'Negocio Teste',
    customer: null,
    contact: null,
    pipeline: null,
    stage: null,
    amount: 1000,
    probability: 40,
    owner: null,
    team: null,
    openedAt: '2026-08-01T12:00:00Z',
    expectedCloseDate: '2026-10-15',
    closedAt: null,
    outcome: 'OPEN' as Opportunity['outcome'],
    winReason: null,
    lossReason: null,
    competitor: null,
    sourceLead: null,
    notes: null,
    createdAt: '2026-08-01T12:00:00Z',
    updatedAt: '2026-08-01T12:00:00Z',
    ...overrides
  };
}

function buildHistoryEntry(overrides: Partial<OpportunityStageHistory> = {}): OpportunityStageHistory {
  return {
    id: 'hist-1',
    fromStage: null,
    toStage: { id: 'stage-1', name: 'Qualificacao', color: '#1e5eff' } as OpportunityStageHistory['toStage'],
    movedByUser: null,
    movedAt: '2026-08-02T12:00:00Z',
    daysInPreviousStage: 3,
    note: null,
    ...overrides
  };
}

describe('OpportunityDetailDrawerComponent', () => {
  let fixture: ComponentFixture<OpportunityDetailDrawerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OpportunityDetailDrawerComponent],
      providers: [provideTranslateService(), provideNoopAnimations()]
    }).compileComponents();

    fixture = TestBed.createComponent(OpportunityDetailDrawerComponent);
    fixture.componentRef.setInput('visible', true);
  });

  it('renders the probability percentage when present', () => {
    fixture.componentRef.setInput('opportunity', buildOpportunity({ probability: 40 }));
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).toContain('40%');
    expect(text).not.toContain('undefined');
  });

  it('falls back to "-" and never prints "undefined" when probability is omitted by the API', () => {
    const opportunity = buildOpportunity();
    delete (opportunity as { probability?: number | null }).probability;

    fixture.componentRef.setInput('opportunity', opportunity);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).not.toContain('undefined');
  });

  it('falls back to "-" when probability is explicitly null', () => {
    fixture.componentRef.setInput('opportunity', buildOpportunity({ probability: null }));
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).not.toContain('undefined');
  });

  it('renders "days in previous stage" for a history entry when present', () => {
    fixture.componentRef.setInput('opportunity', buildOpportunity());
    fixture.componentRef.setInput('history', [buildHistoryEntry({ daysInPreviousStage: 5 })]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).not.toContain('undefined');
  });

  it('never prints "undefined" when daysInPreviousStage is omitted by the API', () => {
    const entry = buildHistoryEntry();
    delete (entry as { daysInPreviousStage?: number | null }).daysInPreviousStage;

    fixture.componentRef.setInput('opportunity', buildOpportunity());
    fixture.componentRef.setInput('history', [entry]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).not.toContain('undefined');
  });
});
