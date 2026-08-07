import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { Pipeline } from '../../../core/models/pipeline.model';
import { PipelineService } from '../../../core/services/pipeline.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { PipelineStagesDialogComponent } from './pipeline-stages-dialog.component';

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

@Component({
  selector: 'app-pipelines-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    ToggleSwitchModule,
    TagModule,
    TooltipModule,
    PipelineStagesDialogComponent
  ],
  templateUrl: './pipelines-page.component.html',
  styleUrl: './pipelines-page.component.scss'
})
export class PipelinesPageComponent {
  private readonly pipelineService = inject(PipelineService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly pipelines = signal<Pipeline[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly dialogVisible = signal(false);
  protected readonly editingPipeline = signal<Pipeline | null>(null);
  protected readonly saving = signal(false);

  protected readonly stagesDialogVisible = signal(false);
  protected readonly stagesPipeline = signal<Pipeline | null>(null);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PIPELINES_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PIPELINES_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('PIPELINES_DELETE'));

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    businessType: ['', [Validators.maxLength(100)]],
    active: [true]
  });

  constructor() {
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.pipelineService
      .list({
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.pipelines.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingPipeline.set(null);
    this.form.reset({ name: '', businessType: '', active: true });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(pipeline: Pipeline): void {
    this.editingPipeline.set(pipeline);
    this.form.reset({
      name: pipeline.name,
      businessType: pipeline.businessType ?? '',
      active: pipeline.active
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
      name: raw.name,
      businessType: raw.businessType || null,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingPipeline();
    const request$ = editing ? this.pipelineService.update(editing.id, request) : this.pipelineService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'settingsPages.pipelines.messages.updated' : 'settingsPages.pipelines.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(pipeline: Pipeline): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: pipeline.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.pipelineService.delete(pipeline.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.pipelines.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected countLabel(value: number): string {
    return String(value);
  }

  protected stageDots(pipeline: Pipeline): string[] {
    return pipeline.stages
      .slice()
      .sort((a, b) => a.displayOrder - b.displayOrder)
      .slice(0, 5)
      .map((stage) => stage.color || 'var(--p-primary-color)');
  }

  protected openStages(pipeline: Pipeline): void {
    this.stagesPipeline.set(pipeline);
    this.stagesDialogVisible.set(true);
  }

  protected onStagesDialogVisibleChange(visible: boolean): void {
    this.stagesDialogVisible.set(visible);
    if (!visible) {
      this.load();
    }
  }
}
