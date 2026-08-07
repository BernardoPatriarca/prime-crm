import { Component, computed, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { SessionStore } from '../../core/store/session.store';

interface DashboardMetric {
  key: string;
  icon: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [TranslatePipe, CardModule, MessageModule, TagModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  private readonly sessionStore = inject(SessionStore);

  protected readonly metrics: DashboardMetric[] = [
    { key: 'leads', icon: 'pi pi-bullseye' },
    { key: 'opportunities', icon: 'pi pi-briefcase' },
    { key: 'customers', icon: 'pi pi-building' },
    { key: 'revenue', icon: 'pi pi-wallet' },
    { key: 'tasks', icon: 'pi pi-check-square' },
    { key: 'conversion', icon: 'pi pi-chart-line' }
  ];

  protected readonly firstName = computed(() => {
    const name = this.sessionStore.user()?.name?.trim();
    if (!name) {
      return '';
    }
    return name.split(/\s+/)[0];
  });
}
