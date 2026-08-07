import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { GenericTableComponent, TableQuery } from './generic-table.component';

interface Row {
  id: string;
  name: string;
}

@Component({
  standalone: true,
  imports: [GenericTableComponent],
  template: `
    <app-generic-table
      [items]="items"
      [totalRecords]="items.length"
      [loading]="loading"
      [columnsCount]="2"
      emptyMessageKey="common.table.empty"
      (queryChange)="onQuery($event)"
    >
      <ng-template #header>
        <tr>
          <th>Nome</th>
          <th>Acoes</th>
        </tr>
      </ng-template>
      <ng-template #body let-item>
        <tr>
          <td>{{ item.name }}</td>
          <td>-</td>
        </tr>
      </ng-template>
    </app-generic-table>
  `
})
class HostComponent {
  items: Row[] = [];
  loading = false;
  lastQuery: TableQuery | null = null;
  queries: TableQuery[] = [];

  onQuery(query: TableQuery): void {
    this.lastQuery = query;
    this.queries.push(query);
  }
}

describe('GenericTableComponent', () => {
  let fixture: ComponentFixture<HostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideNoopAnimations(), provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' })]
    }).compileComponents();

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('shows the empty state when there are no items', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('renders provided rows through the projected body template', () => {
    fixture.componentInstance.items = [{ id: '1', name: 'Item A' }];
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Item A');
  });

  it('does not emit a query on init, leaving the initial load to the page', () => {
    expect(fixture.componentInstance.queries.length).toBe(0);
  });

  it('keeps the table mounted while loading so it never re-triggers a lazy load', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table"]')).toBeTruthy();

    fixture.componentInstance.loading = true;
    fixture.detectChanges();

    expect(compiled.querySelector('[data-testid="generic-table"]')).toBeTruthy();
  });

  it('does not re-emit an identical consecutive query', () => {
    const table = fixture.debugElement.children[0].componentInstance as GenericTableComponent<Row>;
    const event = { first: 0, rows: 10 };

    table['onLazyLoad'](event);
    table['onLazyLoad'](event);
    table['onLazyLoad'](event);

    expect(fixture.componentInstance.queries.length).toBe(1);
  });

  it('emits again when the query actually changes', () => {
    const table = fixture.debugElement.children[0].componentInstance as GenericTableComponent<Row>;

    table['onLazyLoad']({ first: 0, rows: 10 });
    table['onLazyLoad']({ first: 10, rows: 10 });

    expect(fixture.componentInstance.queries.map((q) => q.page)).toEqual([0, 1]);
  });
});
