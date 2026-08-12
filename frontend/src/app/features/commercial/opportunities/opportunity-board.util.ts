import { OpportunityBoard, OpportunityCard } from '../../../core/models/opportunity.model';

export type StageReasonRequirement = 'NONE' | 'LOSS' | 'WIN';

const WON_PROBABILITY = 100;

export interface BoardColumn {
  stageId: string;
  stageName: string;
  displayOrder: number;
  defaultProbability: number | null;
  color: string | null;
  requiresLossReason: boolean;
  totalCount: number;
  totalAmount: number;
  hasMore: boolean;
  cards: OpportunityCard[];
}

export function sumCardAmounts(cards: readonly OpportunityCard[]): number {
  return cards.reduce((total, card) => total + (card.amount ?? 0), 0);
}

export function toBoardColumns(board: OpportunityBoard | null): BoardColumn[] {
  if (!board) {
    return [];
  }
  return [...(board.columns ?? [])]
    .sort((left, right) => left.displayOrder - right.displayOrder)
    .map((column) => {
      const cards = [...(column.opportunities ?? [])];
      return {
        stageId: column.stageId,
        stageName: column.stageName,
        displayOrder: column.displayOrder,
        defaultProbability: column.defaultProbability ?? null,
        color: column.color ?? null,
        requiresLossReason: column.requiresLossReason === true,
        totalCount: column.totalCount ?? cards.length,
        totalAmount: column.totalAmount ?? sumCardAmounts(cards),
        hasMore: column.hasMore === true,
        cards
      };
    });
}

export function boardTotalCount(columns: readonly BoardColumn[]): number {
  return columns.reduce((total, column) => total + column.totalCount, 0);
}

export function boardTotalAmount(columns: readonly BoardColumn[]): number {
  return columns.reduce((total, column) => total + column.totalAmount, 0);
}

export function findCard(
  columns: readonly BoardColumn[],
  stageId: string,
  cardId: string
): OpportunityCard | null {
  const column = columns.find((candidate) => candidate.stageId === stageId);
  return column?.cards.find((card) => card.id === cardId) ?? null;
}

export function indexOfCard(columns: readonly BoardColumn[], stageId: string, cardId: string): number {
  const column = columns.find((candidate) => candidate.stageId === stageId);
  return column ? column.cards.findIndex((card) => card.id === cardId) : -1;
}

export function moveCard(
  columns: readonly BoardColumn[],
  cardId: string,
  fromStageId: string,
  toStageId: string,
  targetIndex: number
): BoardColumn[] {
  const card = findCard(columns, fromStageId, cardId);
  if (!card || fromStageId === toStageId) {
    return [...columns];
  }
  const amount = card.amount ?? 0;

  return columns.map((column) => {
    if (column.stageId === fromStageId) {
      return {
        ...column,
        cards: column.cards.filter((candidate) => candidate.id !== cardId),
        totalCount: Math.max(0, column.totalCount - 1),
        totalAmount: column.totalAmount - amount
      };
    }
    if (column.stageId === toStageId) {
      const cards = [...column.cards];
      const index = Math.max(0, Math.min(targetIndex, cards.length));
      cards.splice(index, 0, card);
      return {
        ...column,
        cards,
        totalCount: column.totalCount + 1,
        totalAmount: column.totalAmount + amount
      };
    }
    return column;
  });
}

export function reasonRequirementFor(column: BoardColumn | null | undefined): StageReasonRequirement {
  if (!column) {
    return 'NONE';
  }
  if (column.requiresLossReason) {
    return 'LOSS';
  }
  if (column.defaultProbability !== null && column.defaultProbability >= WON_PROBABILITY) {
    return 'WIN';
  }
  return 'NONE';
}
