import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface AreaChartPoint {
  label: string;
  value: number;
  secondaryValue?: number;
  tooltip?: string;
}

interface PlottedPoint extends AreaChartPoint {
  x: number;
  y: number;
  secondaryY: number | null;
  showLabel: boolean;
}

const VIEWBOX_WIDTH = 720;
const VIEWBOX_HEIGHT = 220;
const PLOT_TOP = 12;
const PLOT_BOTTOM = 188;
const LABEL_BASELINE = 210;
const GRID_LINES = 4;
const MAX_VISIBLE_LABELS = 8;

@Component({
  selector: 'app-area-chart',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './area-chart.component.html',
  styleUrl: './area-chart.component.scss'
})
export class AreaChartComponent {
  points = input<AreaChartPoint[]>([]);
  gradientId = input('area-chart-gradient');

  protected readonly viewBox = `0 0 ${VIEWBOX_WIDTH} ${VIEWBOX_HEIGHT}`;
  protected readonly baseline = PLOT_BOTTOM;

  protected readonly gridLines = computed(() =>
    Array.from({ length: GRID_LINES + 1 }, (_, index) => PLOT_TOP + ((PLOT_BOTTOM - PLOT_TOP) / GRID_LINES) * index)
  );

  protected readonly plotted = computed<PlottedPoint[]>(() => {
    const source = this.points();
    if (source.length === 0) {
      return [];
    }

    const max = Math.max(
      ...source.map((point) => Math.max(point.value, point.secondaryValue ?? 0)),
      1
    );
    const step = source.length === 1 ? 0 : VIEWBOX_WIDTH / (source.length - 1);
    const labelInterval = Math.ceil(source.length / MAX_VISIBLE_LABELS);

    return source.map((point, index) => ({
      ...point,
      x: source.length === 1 ? VIEWBOX_WIDTH / 2 : index * step,
      y: this.toY(point.value, max),
      secondaryY: point.secondaryValue === undefined ? null : this.toY(point.secondaryValue, max),
      showLabel: index % labelInterval === 0 || index === source.length - 1
    }));
  });

  protected readonly linePath = computed(() => this.toPath(this.plotted().map((point) => [point.x, point.y])));

  protected readonly secondaryPath = computed(() => {
    const points = this.plotted();
    if (points.some((point) => point.secondaryY === null)) {
      return '';
    }
    return this.toPath(points.map((point) => [point.x, point.secondaryY as number]));
  });

  protected readonly areaPath = computed(() => {
    const points = this.plotted();
    if (points.length === 0) {
      return '';
    }
    const first = points[0];
    const last = points[points.length - 1];
    return `${this.linePath()} L ${last.x} ${PLOT_BOTTOM} L ${first.x} ${PLOT_BOTTOM} Z`;
  });

  protected readonly hasData = computed(() => this.points().some((point) => point.value > 0));

  private toY(value: number, max: number): number {
    return PLOT_BOTTOM - (value / max) * (PLOT_BOTTOM - PLOT_TOP);
  }

  private toPath(coordinates: [number, number][]): string {
    return coordinates
      .map(([x, y], index) => `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`)
      .join(' ');
  }

  protected readonly labelBaseline = LABEL_BASELINE;
}
