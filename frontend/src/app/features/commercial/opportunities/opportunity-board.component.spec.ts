import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateService } from '@ngx-translate/core';
import { OpportunityCard } from '../../../core/models/opportunity.model';
import { OpportunityBoardComponent } from './opportunity-board.component';
import { BoardColumn } from './opportunity-board.util';

function buildCard(overrides: Partial<OpportunityCard> = {}): OpportunityCard {
  return {
    id: 'opp-1',
    code: 'OPO-000001',
    title: 'Negocio Teste',
    amount: 1000,
    probability: 40,
    expectedCloseDate: '2026-10-15',
    openedAt: '2026-08-01T12:00:00Z',
    customer: null,
    owner: null,
    ...overrides
  };
}

function buildColumn(overrides: Partial<BoardColumn> = {}): BoardColumn {
  return {
    stageId: 'stage-1',
    stageName: 'Qualificacao',
    displayOrder: 1,
    defaultProbability: 20,
    color: '#1e5eff',
    requiresLossReason: false,
    totalCount: 1,
    totalAmount: 1000,
    hasMore: false,
    cards: [buildCard()],
    ...overrides
  };
}

describe('OpportunityBoardComponent', () => {
  let fixture: ComponentFixture<OpportunityBoardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OpportunityBoardComponent],
      providers: [provideTranslateService()]
    }).compileComponents();

    fixture = TestBed.createComponent(OpportunityBoardComponent);
  });

  it('renders the probability chip when probability is a number', () => {
    fixture.componentRef.setInput('columns', [buildColumn({ cards: [buildCard({ probability: 40 })] })]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).toContain('40%');
    expect(text).not.toContain('undefined');
  });

  it('does not render a probability chip and never prints "undefined" when probability is omitted by the API', () => {
    const cardWithoutProbability = buildCard();
    delete (cardWithoutProbability as { probability?: number | null }).probability;

    fixture.componentRef.setInput('columns', [buildColumn({ cards: [cardWithoutProbability] })]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).not.toContain('undefined');
    expect(text).not.toContain('%');
  });

  it('does not render a probability chip when probability is explicitly null', () => {
    fixture.componentRef.setInput('columns', [buildColumn({ cards: [buildCard({ probability: null })] })]);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).innerText;
    expect(text).not.toContain('undefined');
    expect(text).not.toContain('%');
  });

  it('shows the empty state message when there are no columns', () => {
    fixture.componentRef.setInput('columns', []);
    fixture.detectChanges();

    const empty = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="opportunity-board-empty"]');
    expect(empty).toBeTruthy();
  });
});
