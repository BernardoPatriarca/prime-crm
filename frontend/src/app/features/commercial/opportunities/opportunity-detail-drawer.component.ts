import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { Opportunity, OpportunityStageHistory } from '../../../core/models/opportunity.model';
import { domainChipStyle } from '../../../shared/utils/domain-color.util';
import { formatCurrencyBRL, formatInstant, formatIsoDate } from '../../../shared/utils/format.util';

type OutcomeSeverity = 'success' | 'danger' | 'info';

@Component({
  selector: 'app-opportunity-detail-drawer',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, DrawerModule, TimelineModule, ButtonModule, TagModule, SkeletonModule],
  templateUrl: './opportunity-detail-drawer.component.html',
  styleUrl: './opportunity-detail-drawer.component.scss'
})
export class OpportunityDetailDrawerComponent {
  visible = input(false);
  opportunity = input<Opportunity | null>(null);
  history = input<OpportunityStageHistory[]>([]);
  loading = input(false);
  canEdit = input(false);

  visibleChange = output<boolean>();
  editRequested = output<Opportunity>();

  protected readonly skeletonRows: readonly number[] = [0, 1, 2];

  protected close(): void {
    this.visibleChange.emit(false);
  }

  protected onVisibleChange(visible: boolean): void {
    this.visibleChange.emit(visible);
  }

  protected requestEdit(): void {
    const opportunity = this.opportunity();
    if (opportunity) {
      this.editRequested.emit(opportunity);
    }
  }

  protected outcomeSeverity(outcome: string | undefined): OutcomeSeverity {
    if (outcome === 'WON') {
      return 'success';
    }
    if (outcome === 'LOST') {
      return 'danger';
    }
    return 'info';
  }

  protected chipStyle(color: string | null | undefined): Record<string, string> | null {
    return domainChipStyle(color);
  }

  protected currency(value: number | null | undefined): string {
    return formatCurrencyBRL(value);
  }

  protected date(value: string | null | undefined): string {
    return formatIsoDate(value);
  }

  protected instant(value: string | null | undefined): string {
    return formatInstant(value);
  }
}
