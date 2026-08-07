import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { Holiday } from '../../../core/models/holiday.model';
import { HolidayService } from '../../../core/services/holiday.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

function toIsoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function fromIsoDate(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

@Component({
  selector: 'app-holidays-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputNumberModule,
    DatePickerModule,
    ToggleSwitchModule,
    CheckboxModule,
    TagModule,
    TooltipModule
  ],
  templateUrl: './holidays-page.component.html',
  styleUrl: './holidays-page.component.scss'
})
export class HolidaysPageComponent {
  private readonly holidayService = inject(HolidayService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly holidays = signal<Holiday[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editingHoliday = signal<Holiday | null>(null);
  protected readonly saving = signal(false);
  protected readonly yearFilter = signal<number | null>(null);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('FERIADOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('FERIADOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('FERIADOS_DELETE'));

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    holidayDate: [new Date(), [Validators.required]],
    name: ['', [Validators.required, Validators.maxLength(150)]],
    national: [true],
    active: [true]
  });

  constructor() {
    this.load();
  }

  protected onYearFilterChange(value: number | null): void {
    this.yearFilter.set(value);
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.holidayService
      .list({
        year: this.yearFilter() ?? undefined,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.holidays.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingHoliday.set(null);
    this.form.reset({
      holidayDate: new Date(),
      name: '',
      national: true,
      active: true
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(holiday: Holiday): void {
    this.editingHoliday.set(holiday);
    this.form.reset({
      holidayDate: fromIsoDate(holiday.holidayDate),
      name: holiday.name,
      national: holiday.national,
      active: holiday.active
    });
    this.dialogVisible.set(true);
  }

  protected closeDialog(): void {
    this.dialogVisible.set(false);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request = {
      holidayDate: toIsoDate(raw.holidayDate),
      name: raw.name,
      national: raw.national,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingHoliday();
    const request$ = editing ? this.holidayService.update(editing.id, request) : this.holidayService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'settingsPages.holidays.messages.updated' : 'settingsPages.holidays.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(holiday: Holiday): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: holiday.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.holidayService.delete(holiday.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.holidays.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected dayOf(isoDate: string): string {
    return isoDate.split('-')[2] ?? '';
  }

  protected monthYearOf(isoDate: string): string {
    const [year, month] = isoDate.split('-');
    return `${month}/${year}`;
  }
}
