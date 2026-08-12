import { Component, WritableSignal, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputMaskModule } from 'primeng/inputmask';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { ConfirmationService } from 'primeng/api';
import { AdminUser } from '../../../core/models/admin-user.model';
import { DomainValue } from '../../../core/models/domain-value.model';
import { Lead, LeadConvertRequest, LeadRequest } from '../../../core/models/lead.model';
import { Pipeline, PipelineStage } from '../../../core/models/pipeline.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { LeadService } from '../../../core/services/lead.service';
import { PipelineService } from '../../../core/services/pipeline.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { domainChipStyle } from '../../../shared/utils/domain-color.util';
import { phoneValidator } from '../../../shared/validators/contact.validators';

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

function trimmedOrNull(value: string | null): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length > 0 ? trimmed : null;
}

function toIsoDate(date: Date | null): string | null {
  if (!date) {
    return null;
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function fromIsoDate(iso: string | null): Date | null {
  if (!iso) {
    return null;
  }
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

@Component({
  selector: 'app-leads-page',
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
    InputMaskModule,
    InputNumberModule,
    TextareaModule,
    SelectModule,
    MultiSelectModule,
    DatePickerModule,
    CheckboxModule,
    TagModule,
    ToggleSwitchModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './leads-page.component.html',
  styleUrl: './leads-page.component.scss'
})
export class LeadsPageComponent {
  private readonly leadService = inject(LeadService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly pipelineService = inject(PipelineService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly leads = signal<Lead[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly originFilter = signal<string | null>(null);
  protected readonly statusFilter = signal<string | null>(null);
  protected readonly priorityFilter = signal<string | null>(null);
  protected readonly ownerFilter = signal<string | null>(null);

  protected readonly dialogVisible = signal(false);
  protected readonly editingLead = signal<Lead | null>(null);
  protected readonly saving = signal(false);

  protected readonly convertDialogVisible = signal(false);
  protected readonly convertingLead = signal<Lead | null>(null);
  protected readonly converting = signal(false);

  protected readonly origins = signal<DomainValue[]>([]);
  protected readonly statuses = signal<DomainValue[]>([]);
  protected readonly priorities = signal<DomainValue[]>([]);
  protected readonly tags = signal<DomainValue[]>([]);
  protected readonly clientTypes = signal<DomainValue[]>([]);
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly pipelines = signal<Pipeline[]>([]);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('LEADS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('LEADS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('LEADS_DELETE'));
  protected readonly canConvert = computed(
    () => this.sessionStore.hasPermission('LEADS_EDIT') && this.sessionStore.hasPermission('CLIENTES_CREATE')
  );

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    companyName: ['', [Validators.maxLength(200)]],
    contactName: ['', [Validators.maxLength(200)]],
    email: ['', [Validators.email, Validators.maxLength(180)]],
    phone: ['', [Validators.maxLength(30), phoneValidator]],
    mobile: ['', [Validators.maxLength(30), phoneValidator]],
    originId: [null as string | null],
    statusId: [null as string | null],
    priorityId: [null as string | null],
    campaign: ['', [Validators.maxLength(150)]],
    ownerUserId: [null as string | null],
    pipelineId: [null as string | null],
    stageId: [null as string | null],
    probability: [null as number | null, [Validators.min(0), Validators.max(100)]],
    estimatedValue: [null as number | null],
    expectedCloseDate: [null as Date | null],
    qualificationScore: [null as number | null, [Validators.min(0), Validators.max(100)]],
    tagIds: [[] as string[]],
    notes: [''],
    active: [true]
  });

  protected readonly convertForm = this.formBuilder.nonNullable.group({
    createOpportunity: [true],
    pipelineId: [null as string | null],
    stageId: [null as string | null],
    opportunityTitle: ['', [Validators.maxLength(200)]],
    amount: [null as number | null],
    expectedCloseDate: [null as Date | null],
    clientTypeId: [null as string | null]
  });

  protected readonly formStages = signal<PipelineStage[]>([]);
  protected readonly convertStages = signal<PipelineStage[]>([]);

  constructor() {
    this.load();
    this.loadDomain('LEAD_ORIGIN', this.origins);
    this.loadDomain('GENERIC_STATUS', this.statuses);
    this.loadDomain('PRIORITY', this.priorities);
    this.loadDomain('TAG', this.tags);
    this.loadDomain('CLIENT_TYPE', this.clientTypes);
    this.adminUserService.list({ size: 200, sort: 'name,asc' }).subscribe((response) => this.users.set(response.content));
    this.pipelineService.list({ active: true, size: 100 }).subscribe((response) => this.pipelines.set(response.content));
  }

  private loadDomain(type: string, target: WritableSignal<DomainValue[]>): void {
    this.domainValueService
      .list({ type, active: true, size: 200, sort: 'displayOrder,asc' })
      .subscribe((response) => target.set(response.content));
  }

  private stagesOf(pipelineId: string | null): PipelineStage[] {
    if (!pipelineId) {
      return [];
    }
    return this.pipelines().find((pipeline) => pipeline.id === pipelineId)?.stages ?? [];
  }

  protected onFormPipelineChange(pipelineId: string | null): void {
    this.form.controls.pipelineId.setValue(pipelineId);
    this.form.controls.stageId.setValue(null);
    this.formStages.set(this.stagesOf(pipelineId));
  }

  protected onConvertPipelineChange(pipelineId: string | null): void {
    this.convertForm.controls.pipelineId.setValue(pipelineId);
    this.convertForm.controls.stageId.setValue(null);
    this.convertStages.set(this.stagesOf(pipelineId));
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  protected onFilterChange(): void {
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected clearFilters(): void {
    this.originFilter.set(null);
    this.statusFilter.set(null);
    this.priorityFilter.set(null);
    this.ownerFilter.set(null);
    this.onFilterChange();
  }

  private load(): void {
    this.loading.set(true);
    this.leadService
      .list({
        search: this.lastQuery.search,
        originId: this.originFilter() ?? undefined,
        statusId: this.statusFilter() ?? undefined,
        priorityId: this.priorityFilter() ?? undefined,
        ownerUserId: this.ownerFilter() ?? undefined,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.leads.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingLead.set(null);
    this.formStages.set([]);
    this.form.reset({
      name: '',
      companyName: '',
      contactName: '',
      email: '',
      phone: '',
      mobile: '',
      originId: null,
      statusId: null,
      priorityId: null,
      campaign: '',
      ownerUserId: null,
      pipelineId: null,
      stageId: null,
      probability: null,
      estimatedValue: null,
      expectedCloseDate: null,
      qualificationScore: null,
      tagIds: [],
      notes: '',
      active: true
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(lead: Lead): void {
    this.editingLead.set(lead);
    this.formStages.set(this.stagesOf(lead.pipeline?.id ?? null));
    this.form.reset({
      name: lead.name,
      companyName: lead.companyName ?? '',
      contactName: lead.contactName ?? '',
      email: lead.email ?? '',
      phone: lead.phone ?? '',
      mobile: lead.mobile ?? '',
      originId: lead.origin?.id ?? null,
      statusId: lead.status?.id ?? null,
      priorityId: lead.priority?.id ?? null,
      campaign: lead.campaign ?? '',
      ownerUserId: lead.owner?.id ?? null,
      pipelineId: lead.pipeline?.id ?? null,
      stageId: lead.stage?.id ?? null,
      probability: lead.probability ?? null,
      estimatedValue: lead.estimatedValue ?? null,
      expectedCloseDate: fromIsoDate(lead.expectedCloseDate),
      qualificationScore: lead.qualificationScore ?? null,
      tagIds: lead.tags.map((tag) => tag.id),
      notes: lead.notes ?? '',
      active: lead.active
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
    const request: LeadRequest = {
      name: raw.name.trim(),
      companyName: trimmedOrNull(raw.companyName),
      contactName: trimmedOrNull(raw.contactName),
      email: trimmedOrNull(raw.email),
      phone: trimmedOrNull(raw.phone),
      mobile: trimmedOrNull(raw.mobile),
      originId: raw.originId,
      statusId: raw.statusId,
      priorityId: raw.priorityId,
      campaign: trimmedOrNull(raw.campaign),
      ownerUserId: raw.ownerUserId,
      pipelineId: raw.pipelineId,
      stageId: raw.stageId,
      probability: raw.probability,
      estimatedValue: raw.estimatedValue,
      expectedCloseDate: toIsoDate(raw.expectedCloseDate),
      qualificationScore: raw.qualificationScore,
      tagIds: raw.tagIds,
      notes: trimmedOrNull(raw.notes),
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingLead();
    const request$ = editing ? this.leadService.update(editing.id, request) : this.leadService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'commercialPages.leads.messages.updated' : 'commercialPages.leads.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(lead: Lead): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: lead.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.leadService.delete(lead.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('commercialPages.leads.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected openConvertDialog(lead: Lead): void {
    this.convertingLead.set(lead);
    this.convertStages.set(this.stagesOf(lead.pipeline?.id ?? null));
    this.convertForm.reset({
      createOpportunity: true,
      pipelineId: lead.pipeline?.id ?? null,
      stageId: null,
      opportunityTitle: lead.companyName ?? lead.name,
      amount: lead.estimatedValue ?? null,
      expectedCloseDate: fromIsoDate(lead.expectedCloseDate),
      clientTypeId: null
    });
    this.convertDialogVisible.set(true);
  }

  protected closeConvertDialog(): void {
    this.convertDialogVisible.set(false);
  }

  protected submitConvert(): void {
    const lead = this.convertingLead();
    if (!lead) {
      return;
    }

    const raw = this.convertForm.getRawValue();
    if (raw.createOpportunity && !raw.pipelineId) {
      this.convertForm.controls.pipelineId.markAsTouched();
      this.convertForm.controls.pipelineId.setErrors({ required: true });
      return;
    }

    const request: LeadConvertRequest = {
      createOpportunity: raw.createOpportunity,
      clientTypeId: raw.clientTypeId,
      ownerUserId: lead.owner?.id ?? null,
      pipelineId: raw.createOpportunity ? raw.pipelineId : null,
      stageId: raw.createOpportunity ? raw.stageId : null,
      opportunityTitle: raw.createOpportunity ? trimmedOrNull(raw.opportunityTitle) : null,
      amount: raw.createOpportunity ? raw.amount : null,
      expectedCloseDate: raw.createOpportunity ? toIsoDate(raw.expectedCloseDate) : null
    };

    this.converting.set(true);
    this.leadService.convert(lead.id, request).subscribe({
      next: (response) => {
        this.converting.set(false);
        this.convertDialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant('commercialPages.leads.messages.converted'),
          detail: response.customer?.name ?? undefined
        });
        this.load();
      },
      error: () => this.converting.set(false)
    });
  }

  protected chipStyle(color: string | null): Record<string, string> | null {
    return domainChipStyle(color);
  }

  protected formatCurrency(value: number | null): string {
    if (value === null || value === undefined) {
      return '-';
    }
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
  }

  protected formatDate(iso: string | null): string {
    if (!iso) {
      return '-';
    }
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }
}
