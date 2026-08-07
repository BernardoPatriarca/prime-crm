import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Injector,
  OnDestroy,
  TemplateRef,
  computed,
  contentChild,
  inject,
  input,
  output,
  signal,
  viewChild
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SkeletonModule } from 'primeng/skeleton';
import { Table, TableLazyLoadEvent, TableModule, TableRowReorderEvent } from 'primeng/table';

export interface TableQuery {
  page: number;
  size: number;
  sortField?: string;
  sortOrder?: number;
  search?: string;
}

const SEARCH_DEBOUNCE_MS = 350;

function queryKey(query: TableQuery): string {
  return [query.page, query.size, query.sortField ?? '', query.sortOrder ?? '', query.search ?? ''].join('|');
}

@Component({
  selector: 'app-generic-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    TranslatePipe,
    TableModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    SkeletonModule
  ],
  templateUrl: './generic-table.component.html',
  styleUrl: './generic-table.component.scss'
})
export class GenericTableComponent<T> implements OnDestroy {
  items = input<T[]>([]);
  totalRecords = input(0);
  loading = input(false);
  rows = input(10);
  rowsPerPageOptions = input<number[]>([10, 20, 50]);
  searchable = input(true);
  searchPlaceholderKey = input('common.table.searchPlaceholder');
  emptyMessageKey = input('common.table.empty');
  emptyIcon = input('pi-inbox');
  columnsCount = input(1);
  dataKey = input('id');

  queryChange = output<TableQuery>();
  rowReorder = output<TableRowReorderEvent>();

  protected readonly headerTemplate = contentChild<TemplateRef<unknown>>('header');
  protected readonly bodyTemplate = contentChild<TemplateRef<unknown>>('body');
  private readonly tableRef = viewChild(Table);
  private readonly environmentInjector = inject(Injector);

  protected readonly templateInjector = computed<Injector | undefined>(() => {
    const table = this.tableRef();
    if (!table) {
      return undefined;
    }
    return Injector.create({ providers: [{ provide: Table, useValue: table }], parent: this.environmentInjector });
  });

  protected readonly searchTerm = signal('');
  protected readonly skeletonRows: readonly number[] = [0, 1, 2, 3, 4];

  protected readonly skeletonColumns = computed(() =>
    Array.from({ length: Math.max(1, Math.min(this.columnsCount(), 8)) }, (_, index) => index)
  );

  private lastSortField?: string;
  private lastSortOrder?: number;
  private lastEmittedKey?: string;
  private searchDebounce?: ReturnType<typeof setTimeout>;

  ngOnDestroy(): void {
    if (this.searchDebounce) {
      clearTimeout(this.searchDebounce);
    }
  }

  protected onSearchInput(value: string): void {
    this.searchTerm.set(value);
    if (this.searchDebounce) {
      clearTimeout(this.searchDebounce);
    }
    this.searchDebounce = setTimeout(() => {
      this.emit({
        page: 0,
        size: this.rows(),
        sortField: this.lastSortField,
        sortOrder: this.lastSortOrder,
        search: value.trim() || undefined
      });
    }, SEARCH_DEBOUNCE_MS);
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const size = event.rows ?? this.rows();
    const page = size > 0 ? Math.floor((event.first ?? 0) / size) : 0;
    const sortField = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
    this.lastSortField = sortField ?? undefined;
    this.lastSortOrder = event.sortOrder ?? undefined;
    this.emit({
      page,
      size,
      sortField: this.lastSortField,
      sortOrder: this.lastSortOrder,
      search: this.searchTerm().trim() || undefined
    });
  }

  protected onRowReorder(event: TableRowReorderEvent): void {
    this.rowReorder.emit(event);
  }

  private emit(query: TableQuery): void {
    const key = queryKey(query);
    if (key === this.lastEmittedKey) {
      return;
    }
    this.lastEmittedKey = key;
    this.queryChange.emit(query);
  }
}
