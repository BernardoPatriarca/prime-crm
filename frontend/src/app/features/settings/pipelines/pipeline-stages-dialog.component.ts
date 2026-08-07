import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ColorPickerModule } from 'primeng/colorpicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule, TableRowReorderEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { PipelineStage } from '../../../core/models/pipeline.model';
import { PipelineStageService } from '../../../core/services/pipeline-stage.service';
import { SessionStore } from '../../../core/store/session.store';

@Component({
  selector: 'app-pipeline-stages-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    DialogModule,
    TableModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    ColorPickerModule,
    CheckboxModule,
    ProgressBarModule,
    TagModule,
    TooltipModule
  ],
  templateUrl: './pipeline-stages-dialog.component.html',
  styleUrl: './pipeline-stages-dialog.component.scss'
})
export class PipelineStagesDialogComponent {
  visible = input(false);
  pipelineId = input<string | null>(null);
  pipelineName = input('');

  visibleChange = output<boolean>();

  private readonly pipelineStageService = inject(PipelineStageService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly stages = signal<PipelineStage[]>([]);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly formVisible = signal(false);
  protected readonly editingStage = signal<PipelineStage | null>(null);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('PIPELINES_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('PIPELINES_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('PIPELINES_DELETE'));

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    displayOrder: [0],
    defaultProbability: [0, [Validators.min(0), Validators.max(100)]],
    slaDays: [null as number | null],
    color: ['#3B82F6'],
    requiresLossReason: [false]
  });

  constructor() {
    effect(() => {
      if (this.visible() && this.pipelineId()) {
        this.load();
      }
    });
  }

  private load(): void {
    const pipelineId = this.pipelineId();
    if (!pipelineId) {
      return;
    }
    this.loading.set(true);
    this.pipelineStageService.list(pipelineId).subscribe({
      next: (response) => {
        this.stages.set(response.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  protected close(): void {
    this.visibleChange.emit(false);
  }

  protected openCreateForm(): void {
    this.editingStage.set(null);
    this.form.reset({
      name: '',
      displayOrder: this.stages().length,
      defaultProbability: 0,
      slaDays: null,
      color: '#3B82F6',
      requiresLossReason: false
    });
    this.formVisible.set(true);
  }

  protected openEditForm(stage: PipelineStage): void {
    this.editingStage.set(stage);
    this.form.reset({
      name: stage.name,
      displayOrder: stage.displayOrder,
      defaultProbability: stage.defaultProbability ?? 0,
      slaDays: stage.slaDays,
      color: stage.color ?? '#3B82F6',
      requiresLossReason: stage.requiresLossReason
    });
    this.formVisible.set(true);
  }

  protected closeForm(): void {
    this.formVisible.set(false);
  }

  protected save(): void {
    const pipelineId = this.pipelineId();
    if (!pipelineId || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request = {
      name: raw.name,
      displayOrder: raw.displayOrder,
      defaultProbability: raw.defaultProbability,
      slaDays: raw.slaDays,
      color: raw.color || null,
      requiresLossReason: raw.requiresLossReason
    };

    this.saving.set(true);
    const editing = this.editingStage();
    const request$ = editing
      ? this.pipelineStageService.update(pipelineId, editing.id, request)
      : this.pipelineStageService.create(pipelineId, request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'settingsPages.pipelines.stages.messages.updated' : 'settingsPages.pipelines.stages.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(stage: PipelineStage): void {
    const pipelineId = this.pipelineId();
    if (!pipelineId) {
      return;
    }
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: stage.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.pipelineStageService.delete(pipelineId, stage.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('settingsPages.pipelines.stages.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected onRowReorder(event: TableRowReorderEvent): void {
    const pipelineId = this.pipelineId();
    if (!pipelineId || event.dragIndex === undefined || event.dropIndex === undefined) {
      return;
    }
    const items = this.stages().map((stage, index) => ({ id: stage.id, displayOrder: index }));
    this.pipelineStageService.reorder(pipelineId, items).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.translate.instant('settingsPages.pipelines.stages.messages.reordered')
      });
      this.load();
    });
  }
}
