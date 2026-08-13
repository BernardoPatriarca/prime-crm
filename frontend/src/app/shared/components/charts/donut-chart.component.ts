import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface DonutSegment {
  label: string;
  value: number;
  color: string;
}

interface PlottedSegment extends DonutSegment {
  dashArray: string;
  dashOffset: number;
  share: number;
}

const RADIUS = 60;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

@Component({
  selector: 'app-donut-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './donut-chart.component.html',
  styleUrl: './donut-chart.component.scss'
})
export class DonutChartComponent {
  segments = input<DonutSegment[]>([]);
  centerValue = input('');
  centerLabel = input('');

  protected readonly radius = RADIUS;

  protected readonly total = computed(() =>
    this.segments().reduce((sum, segment) => sum + segment.value, 0)
  );

  protected readonly plotted = computed<PlottedSegment[]>(() => {
    const total = this.total();
    if (total === 0) {
      return [];
    }

    let consumed = 0;
    return this.segments()
      .filter((segment) => segment.value > 0)
      .map((segment) => {
        const length = (segment.value / total) * CIRCUMFERENCE;
        const plotted: PlottedSegment = {
          ...segment,
          dashArray: `${length} ${CIRCUMFERENCE - length}`,
          dashOffset: -consumed,
          share: Math.round((segment.value / total) * 100)
        };
        consumed += length;
        return plotted;
      });
  });
}
