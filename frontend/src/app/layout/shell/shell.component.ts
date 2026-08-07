import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { LayoutStore } from '../../core/store/layout.store';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, TopbarComponent, SidebarComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly router = inject(Router);
  private readonly layoutStore = inject(LayoutStore);

  private readonly content = viewChild<ElementRef<HTMLElement>>('content');

  protected readonly contentScrolled = signal(false);
  protected readonly routeAnimationKey = signal(0);

  constructor() {
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe(() => {
        this.layoutStore.closeTransientSidebar();
        this.routeAnimationKey.update((key) => key + 1);
        const element = this.content()?.nativeElement;
        if (element) {
          element.scrollTop = 0;
        }
        this.contentScrolled.set(false);
      });
  }

  protected onContentScroll(event: Event): void {
    const element = event.target as HTMLElement;
    this.contentScrolled.set(element.scrollTop > 4);
  }
}
