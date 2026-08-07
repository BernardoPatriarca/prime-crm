import { Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { TagModule } from 'primeng/tag';

@Component({
  selector: 'app-under-construction',
  standalone: true,
  imports: [TranslatePipe, TagModule],
  templateUrl: './under-construction.component.html',
  styleUrl: './under-construction.component.scss'
})
export class UnderConstructionComponent {
  titleKey = input.required<string>();
  descriptionKey = input.required<string>();
  descriptionParams = input<Record<string, unknown> | undefined>(undefined);
}
