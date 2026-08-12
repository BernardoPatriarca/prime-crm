import { OpportunityBoard, OpportunityCard } from '../../../core/models/opportunity.model';
import {
  boardTotalAmount,
  boardTotalCount,
  findCard,
  indexOfCard,
  moveCard,
  reasonRequirementFor,
  sumCardAmounts,
  toBoardColumns
} from './opportunity-board.util';

function buildCard(overrides: Partial<OpportunityCard> = {}): OpportunityCard {
  return {
    id: 'opp-1',
    code: 'OPO-000001',
    title: 'Negócio Teste',
    amount: 1000,
    probability: 20,
    expectedCloseDate: '2026-10-15',
    openedAt: '2026-08-01T12:00:00Z',
    customer: { id: 'cus-1', code: 'CLI-000001', name: 'Cliente Teste' },
    owner: { id: 'usr-1', name: 'Ana Souza', email: 'ana@teste.com' },
    ...overrides
  };
}

function buildBoard(): OpportunityBoard {
  return {
    pipelineId: 'pipe-1',
    pipelineName: 'Funil Padrão',
    limitPerStage: 50,
    totalCount: 3,
    totalAmount: 6000,
    columns: [
      {
        stageId: 'stage-2',
        stageName: 'Proposta',
        displayOrder: 2,
        defaultProbability: 60,
        color: '#f59e0b',
        requiresLossReason: false,
        totalCount: 1,
        totalAmount: 3000,
        hasMore: false,
        opportunities: [buildCard({ id: 'opp-3', amount: 3000 })]
      },
      {
        stageId: 'stage-1',
        stageName: 'Qualificação',
        displayOrder: 1,
        defaultProbability: 20,
        color: '#1e5eff',
        requiresLossReason: false,
        totalCount: 2,
        totalAmount: 3000,
        hasMore: false,
        opportunities: [buildCard({ id: 'opp-1', amount: 1000 }), buildCard({ id: 'opp-2', amount: 2000 })]
      }
    ]
  };
}

describe('opportunity-board.util', () => {
  describe('toBoardColumns', () => {
    it('groups the opportunities per stage following the display order', () => {
      const columns = toBoardColumns(buildBoard());

      expect(columns.map((column) => column.stageId)).toEqual(['stage-1', 'stage-2']);
      expect(columns[0].cards.map((card) => card.id)).toEqual(['opp-1', 'opp-2']);
      expect(columns[1].cards.map((card) => card.id)).toEqual(['opp-3']);
    });

    it('returns an empty board when there is no response', () => {
      expect(toBoardColumns(null)).toEqual([]);
    });

    it('keeps the server totals for each column', () => {
      const columns = toBoardColumns(buildBoard());

      expect(columns[0].totalCount).toBe(2);
      expect(columns[0].totalAmount).toBe(3000);
      expect(columns[1].totalCount).toBe(1);
      expect(columns[1].totalAmount).toBe(3000);
    });

    it('falls back to the card sum when the server omits the column amount', () => {
      const board = buildBoard();
      board.columns[1].totalAmount = null;

      const columns = toBoardColumns(board);

      expect(columns[0].totalAmount).toBe(3000);
    });

    it('treats cards without an amount as zero', () => {
      expect(sumCardAmounts([buildCard({ amount: null }), buildCard({ amount: 250 })])).toBe(250);
    });
  });

  describe('board totals', () => {
    it('adds up the column totalizers', () => {
      const columns = toBoardColumns(buildBoard());

      expect(boardTotalCount(columns)).toBe(3);
      expect(boardTotalAmount(columns)).toBe(6000);
    });
  });

  describe('moveCard', () => {
    it('moves the card and recalculates both column totalizers', () => {
      const columns = toBoardColumns(buildBoard());

      const moved = moveCard(columns, 'opp-1', 'stage-1', 'stage-2', 0);

      expect(moved[0].cards.map((card) => card.id)).toEqual(['opp-2']);
      expect(moved[0].totalCount).toBe(1);
      expect(moved[0].totalAmount).toBe(2000);
      expect(moved[1].cards.map((card) => card.id)).toEqual(['opp-1', 'opp-3']);
      expect(moved[1].totalCount).toBe(2);
      expect(moved[1].totalAmount).toBe(4000);
    });

    it('restores the exact original state when the move is undone', () => {
      const columns = toBoardColumns(buildBoard());
      const originalIndex = indexOfCard(columns, 'stage-1', 'opp-2');

      const moved = moveCard(columns, 'opp-2', 'stage-1', 'stage-2', 0);
      const reverted = moveCard(moved, 'opp-2', 'stage-2', 'stage-1', originalIndex);

      expect(reverted[0].cards.map((card) => card.id)).toEqual(['opp-1', 'opp-2']);
      expect(reverted[0].totalCount).toBe(2);
      expect(reverted[0].totalAmount).toBe(3000);
      expect(reverted[1].cards.map((card) => card.id)).toEqual(['opp-3']);
      expect(reverted[1].totalCount).toBe(1);
      expect(reverted[1].totalAmount).toBe(3000);
    });

    it('does not change anything when the source and target stages are the same', () => {
      const columns = toBoardColumns(buildBoard());

      const moved = moveCard(columns, 'opp-1', 'stage-1', 'stage-1', 1);

      expect(moved[0].cards.map((card) => card.id)).toEqual(['opp-1', 'opp-2']);
      expect(moved[0].totalCount).toBe(2);
    });

    it('ignores an unknown card', () => {
      const columns = toBoardColumns(buildBoard());

      const moved = moveCard(columns, 'ghost', 'stage-1', 'stage-2', 0);

      expect(moved[0].totalCount).toBe(2);
      expect(moved[1].totalCount).toBe(1);
    });

    it('clamps the target index inside the destination column', () => {
      const columns = toBoardColumns(buildBoard());

      const moved = moveCard(columns, 'opp-1', 'stage-1', 'stage-2', 99);

      expect(moved[1].cards.map((card) => card.id)).toEqual(['opp-3', 'opp-1']);
    });
  });

  describe('findCard and indexOfCard', () => {
    it('locates a card inside its column', () => {
      const columns = toBoardColumns(buildBoard());

      expect(findCard(columns, 'stage-1', 'opp-2')?.amount).toBe(2000);
      expect(indexOfCard(columns, 'stage-1', 'opp-2')).toBe(1);
      expect(findCard(columns, 'stage-1', 'ghost')).toBeNull();
      expect(indexOfCard(columns, 'ghost-stage', 'opp-1')).toBe(-1);
    });
  });

  describe('reasonRequirementFor', () => {
    it('requires the loss reason when the stage demands it', () => {
      const columns = toBoardColumns(buildBoard());
      columns[1].requiresLossReason = true;

      expect(reasonRequirementFor(columns[1])).toBe('LOSS');
    });

    it('requires the win reason on a hundred percent stage', () => {
      const columns = toBoardColumns(buildBoard());
      columns[1].defaultProbability = 100;

      expect(reasonRequirementFor(columns[1])).toBe('WIN');
    });

    it('prefers the loss reason when the stage is both a loss stage and a hundred percent stage', () => {
      const columns = toBoardColumns(buildBoard());
      columns[1].requiresLossReason = true;
      columns[1].defaultProbability = 100;

      expect(reasonRequirementFor(columns[1])).toBe('LOSS');
    });

    it('requires nothing on a regular stage', () => {
      const columns = toBoardColumns(buildBoard());

      expect(reasonRequirementFor(columns[0])).toBe('NONE');
      expect(reasonRequirementFor(null)).toBe('NONE');
    });
  });
});
