import { CdkDrag, CdkDragDrop, CdkDragPlaceholder, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';
import { OpportunityCard } from '../../../core/models/opportunity.model';
import { domainChipStyle } from '../../../shared/utils/domain-color.util';
import { formatCompactCurrencyBRL, formatCurrencyBRL, formatIsoDate, initialsOf } from '../../../shared/utils/format.util';
import { BoardColumn } from './opportunity-board.util';

export interface BoardDropEvent {
  cardId: string;
  fromStageId: string;
  toStageId: string;
  targetIndex: number;
}

@Component({
  selector: 'app-opportunity-board',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    CdkDropListGroup,
    CdkDropList,
    CdkDrag,
    CdkDragPlaceholder,
    AvatarModule,
    BadgeModule,
    SkeletonModule,
    TooltipModule
  ],
  templateUrl: './opportunity-board.component.html',
  styleUrl: './opportunity-board.component.scss'
})
export class OpportunityBoardComponent {
  columns = input<BoardColumn[]>([]);
  canEdit = input(false);
  loading = input(false);
  movingCardId = input<string | null>(null);

  cardDropped = output<BoardDropEvent>();
  cardSelected = output<OpportunityCard>();

  protected readonly skeletonColumns: readonly number[] = [0, 1, 2, 3];
  protected readonly skeletonCards: readonly number[] = [0, 1, 2];

  protected onDrop(event: CdkDragDrop<BoardColumn>): void {
    const fromColumn = event.previousContainer.data;
    const toColumn = event.container.data;
    const card = fromColumn?.cards[event.previousIndex];
    if (!fromColumn || !toColumn || !card || fromColumn.stageId === toColumn.stageId) {
      return;
    }
    this.cardDropped.emit({
      cardId: card.id,
      fromStageId: fromColumn.stageId,
      toStageId: toColumn.stageId,
      targetIndex: event.currentIndex
    });
  }

  protected stageDotStyle(color: string | null): Record<string, string> {
    return { background: color ?? 'var(--p-primary-color, #1e5eff)' };
  }

  protected chipStyle(color: string | null): Record<string, string> | null {
    return domainChipStyle(color);
  }

  protected currency(value: number | null): string {
    return formatCurrencyBRL(value);
  }

  protected compactCurrency(value: number | null): string {
    return formatCompactCurrencyBRL(value, formatCurrencyBRL(0));
  }

  protected date(value: string | null): string {
    return formatIsoDate(value);
  }

  protected initials(name: string | null | undefined): string {
    return initialsOf(name);
  }
}
