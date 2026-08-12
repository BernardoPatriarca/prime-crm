import { Component, WritableSignal, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { AdminUser } from '../../../core/models/admin-user.model';
import { Customer } from '../../../core/models/customer.model';
import { DomainValue } from '../../../core/models/domain-value.model';
import {
  Opportunity,
  OpportunityCard,
  OpportunityOutcome,
  OpportunityRequest,
  OpportunityStageHistory,
  OpportunityStageMoveRequest
} from '../../../core/models/opportunity.model';
import { Pipeline, PipelineStage } from '../../../core/models/pipeline.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { ContactService } from '../../../core/services/contact.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { PipelineService } from '../../../core/services/pipeline.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { domainChipStyle } from '../../../shared/utils/domain-color.util';
import { formatCurrencyBRL, formatIsoDate, initialsOf } from '../../../shared/utils/format.util';
import { BoardDropEvent, OpportunityBoardComponent } from './opportunity-board.component';
import {
  BoardColumn,
  StageReasonRequirement,
  boardTotalAmount,
  boardTotalCount,
  findCard,
  indexOfCard,
  moveCard,
  reasonRequirementFor,
  toBoardColumns
} from './opportunity-board.util';
import { OpportunityDetailDrawerComponent } from './opportunity-detail-drawer.component';

export type OpportunityView = 'BOARD' | 'LIST';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const CUSTOMER_OPTIONS_SIZE = 50;
const BOARD_LIMIT_PER_STAGE = 50;

interface PendingMove {
  cardId: string;
  cardTitle: string;
  fromStageId: string;
  fromStageName: string;
  toStageId: string;
  sourceIndex: number;
}

interface CustomerOption {
  id: string;
  name: string;
  code: string;
}

interface ContactOption {
  id: string;
  name: string;
}

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
  const [year, month, day] = iso.slice(0, 10).split('-').map(Number);
  return new Date(year, month - 1, day);
}

@Component({
  selector: 'app-opportunities-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    OpportunityBoardComponent,
    OpportunityDetailDrawerComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputNumberModule,
    TextareaModule,
    SelectModule,
    SelectButtonModule,
    DatePickerModule,
    TagModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './opportunities-page.component.html',
  styleUrl: './opportunities-page.component.scss'
})
export class OpportunitiesPageComponent {
  private readonly opportunityService = inject(OpportunityService);
  private readonly pipelineService = inject(PipelineService);
  private readonly customerService = inject(CustomerService);
  private readonly contactService = inject(ContactService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly view = signal<OpportunityView>('BOARD');
  protected readonly pipelines = signal<Pipeline[]>([]);
  protected readonly selectedPipelineId = signal<string | null>(null);

  protected readonly boardColumns = signal<BoardColumn[]>([]);
  protected readonly boardLoading = signal(false);
  protected readonly movingCardId = signal<string | null>(null);

  protected readonly opportunities = signal<Opportunity[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly stageFilter = signal<string | null>(null);
  protected readonly ownerFilter = signal<string | null>(null);
  protected readonly customerFilter = signal<string | null>(null);
  protected readonly outcomeFilter = signal<OpportunityOutcome | null>(null);

  protected readonly dialogVisible = signal(false);
  protected readonly editingOpportunity = signal<Opportunity | null>(null);
  protected readonly saving = signal(false);

  protected readonly reasonDialogVisible = signal(false);
  protected readonly reasonRequirement = signal<StageReasonRequirement>('NONE');
  protected readonly pendingMove = signal<PendingMove | null>(null);

  protected readonly detailVisible = signal(false);
  protected readonly detailOpportunity = signal<Opportunity | null>(null);
  protected readonly detailHistory = signal<OpportunityStageHistory[]>([]);
  protected readonly detailLoading = signal(false);

  protected readonly customerOptions = signal<CustomerOption[]>([]);
  protected readonly contactOptions = signal<ContactOption[]>([]);
  protected readonly users = signal<AdminUser[]>([]);
  protected readonly teams = signal<DomainValue[]>([]);
  protected readonly winReasons = signal<DomainValue[]>([]);
  protected readonly lossReasons = signal<DomainValue[]>([]);
  protected readonly formStages = signal<PipelineStage[]>([]);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('OPORTUNIDADES_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('OPORTUNIDADES_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('OPORTUNIDADES_DELETE'));

  protected readonly boardCount = computed(() => boardTotalCount(this.boardColumns()));
  protected readonly boardAmount = computed(() => boardTotalAmount(this.boardColumns()));

  protected readonly pipelineStages = computed<PipelineStage[]>(() => {
    const pipelineId = this.selectedPipelineId();
    if (!pipelineId) {
      return [];
    }
    return this.pipelines().find((pipeline) => pipeline.id === pipelineId)?.stages ?? [];
  });

  protected readonly reasonOptions = computed<DomainValue[]>(() =>
    this.reasonRequirement() === 'LOSS' ? this.lossReasons() : this.winReasons()
  );

  protected readonly viewOptions = computed(() => {
    this.translate.currentLang();
    return [
      { value: 'BOARD' as const, icon: 'pi pi-th-large', label: this.translate.instant('commercialPages.opportunities.views.board') },
      { value: 'LIST' as const, icon: 'pi pi-list', label: this.translate.instant('commercialPages.opportunities.views.list') }
    ];
  });

  protected readonly outcomeOptions = computed(() => {
    this.translate.currentLang();
    return (['OPEN', 'WON', 'LOST'] as const).map((value) => ({
      value,
      label: this.translate.instant(`commercialPages.opportunities.outcome.${value}`)
    }));
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private listLoaded = false;
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    customerId: [null as string | null, [Validators.required]],
    contactId: [null as string | null],
    pipelineId: [null as string | null, [Validators.required]],
    stageId: [null as string | null],
    amount: [null as number | null],
    probability: [null as number | null, [Validators.min(0), Validators.max(100)]],
    ownerUserId: [null as string | null],
    teamId: [null as string | null],
    expectedCloseDate: [null as Date | null],
    competitor: ['', [Validators.maxLength(200)]],
    notes: ['']
  });

  protected readonly reasonForm = this.formBuilder.nonNullable.group({
    reasonId: [null as string | null],
    note: ['']
  });

  constructor() {
    this.customerSearch
      .pipe(
        debounceTime(CUSTOMER_SEARCH_DEBOUNCE_MS),
        switchMap((search) =>
          this.customerService.list({ search: search || undefined, size: CUSTOMER_OPTIONS_SIZE, sort: 'name,asc' })
        ),
        takeUntilDestroyed()
      )
      .subscribe((response) => this.customerOptions.set(response.content.map(toCustomerOption)));

    this.pipelineService.list({ active: true, size: 100, sort: 'name,asc' }).subscribe((response) => {
      this.pipelines.set(response.content);
      const current = this.selectedPipelineId();
      if (!current && response.content.length > 0) {
        this.selectedPipelineId.set(response.content[0].id);
        this.loadBoard();
      }
    });

    this.customerService
      .list({ size: CUSTOMER_OPTIONS_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.customerOptions.set(response.content.map(toCustomerOption)));
    this.adminUserService.list({ size: 200, sort: 'name,asc' }).subscribe((response) => this.users.set(response.content));
    this.loadDomain('TEAM', this.teams);
    this.loadDomain('WIN_REASON', this.winReasons);
    this.loadDomain('LOSS_REASON', this.lossReasons);
  }

  private loadDomain(type: string, target: WritableSignal<DomainValue[]>): void {
    this.domainValueService
      .list({ type, active: true, size: 200, sort: 'displayOrder,asc' })
      .subscribe((response) => target.set(response.content));
  }

  protected onViewChange(view: OpportunityView | null): void {
    if (!view) {
      return;
    }
    this.view.set(view);
    if (view === 'LIST' && !this.listLoaded) {
      this.loadList();
    }
  }

  protected onPipelineChange(pipelineId: string | null): void {
    this.selectedPipelineId.set(pipelineId);
    this.stageFilter.set(null);
    this.loadBoard();
    if (this.listLoaded) {
      this.lastQuery = { ...this.lastQuery, page: 0 };
      this.loadList();
    }
  }

  protected loadBoard(): void {
    const pipelineId = this.selectedPipelineId();
    if (!pipelineId) {
      this.boardColumns.set([]);
      return;
    }
    this.boardLoading.set(true);
    this.opportunityService.board(pipelineId, BOARD_LIMIT_PER_STAGE).subscribe({
      next: (board) => {
        this.boardColumns.set(toBoardColumns(board));
        this.boardLoading.set(false);
      },
      error: () => this.boardLoading.set(false)
    });
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.loadList();
  }

  protected onFilterChange(): void {
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.loadList();
  }

  protected clearFilters(): void {
    this.stageFilter.set(null);
    this.ownerFilter.set(null);
    this.customerFilter.set(null);
    this.outcomeFilter.set(null);
    this.onFilterChange();
  }

  private loadList(): void {
    this.listLoaded = true;
    this.loading.set(true);
    this.opportunityService
      .list({
        search: this.lastQuery.search,
        pipelineId: this.selectedPipelineId() ?? undefined,
        stageId: this.stageFilter() ?? undefined,
        ownerUserId: this.ownerFilter() ?? undefined,
        customerId: this.customerFilter() ?? undefined,
        outcome: this.outcomeFilter() ?? undefined,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.opportunities.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected onCardDropped(event: BoardDropEvent): void {
    const columns = this.boardColumns();
    const card = findCard(columns, event.fromStageId, event.cardId);
    const sourceColumn = columns.find((column) => column.stageId === event.fromStageId);
    const targetColumn = columns.find((column) => column.stageId === event.toStageId);
    if (!card || !sourceColumn || !targetColumn) {
      return;
    }

    const pending: PendingMove = {
      cardId: card.id,
      cardTitle: card.title,
      fromStageId: sourceColumn.stageId,
      fromStageName: sourceColumn.stageName,
      toStageId: targetColumn.stageId,
      sourceIndex: indexOfCard(columns, event.fromStageId, event.cardId)
    };

    this.boardColumns.set(moveCard(columns, event.cardId, event.fromStageId, event.toStageId, event.targetIndex));

    const requirement = reasonRequirementFor(targetColumn);
    if (requirement === 'NONE') {
      this.commitMove(pending, {});
      return;
    }

    this.reasonRequirement.set(requirement);
    this.pendingMove.set(pending);
    this.reasonForm.reset({ reasonId: null, note: '' });
    this.reasonDialogVisible.set(true);
  }

  protected confirmReason(): void {
    const pending = this.pendingMove();
    if (!pending) {
      return;
    }
    const reasonId = this.reasonForm.controls.reasonId.value;
    if (!reasonId) {
      this.reasonForm.controls.reasonId.markAsTouched();
      this.reasonForm.controls.reasonId.setErrors({ required: true });
      return;
    }

    const note = trimmedOrNull(this.reasonForm.controls.note.value);
    const reason: Partial<OpportunityStageMoveRequest> =
      this.reasonRequirement() === 'LOSS' ? { lossReasonId: reasonId } : { winReasonId: reasonId };

    this.pendingMove.set(null);
    this.reasonDialogVisible.set(false);
    this.commitMove(pending, { ...reason, note });
  }

  protected cancelReason(): void {
    this.reasonDialogVisible.set(false);
    this.discardPendingMove();
  }

  protected discardPendingMove(): void {
    const pending = this.pendingMove();
    if (!pending) {
      return;
    }
    this.pendingMove.set(null);
    this.revertMove(pending);
  }

  private revertMove(pending: PendingMove): void {
    this.boardColumns.set(
      moveCard(this.boardColumns(), pending.cardId, pending.toStageId, pending.fromStageId, pending.sourceIndex)
    );
  }

  private commitMove(pending: PendingMove, reason: Partial<OpportunityStageMoveRequest>): void {
    this.movingCardId.set(pending.cardId);
    this.opportunityService.moveStage(pending.cardId, { stageId: pending.toStageId, ...reason }).subscribe({
      next: () => {
        this.movingCardId.set(null);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant('commercialPages.opportunities.messages.moved'),
          detail: pending.cardTitle
        });
        this.loadBoard();
        if (this.listLoaded) {
          this.loadList();
        }
      },
      error: () => {
        this.movingCardId.set(null);
        this.revertMove(pending);
        this.messageService.add({
          severity: 'warn',
          summary: this.translate.instant('commercialPages.opportunities.messages.moveFailed'),
          detail: this.translate.instant('commercialPages.opportunities.messages.moveReverted', {
            stage: pending.fromStageName
          })
        });
      }
    });
  }

  protected onCardSelected(card: OpportunityCard): void {
    this.openDetail(card.id);
  }

  protected openDetail(id: string): void {
    this.detailOpportunity.set(null);
    this.detailHistory.set([]);
    this.detailLoading.set(true);
    this.detailVisible.set(true);

    this.opportunityService.getById(id).subscribe({
      next: (opportunity) => {
        this.detailOpportunity.set(opportunity);
        this.detailLoading.set(false);
      },
      error: () => this.detailLoading.set(false)
    });
    this.opportunityService.history(id).subscribe({
      next: (history) => this.detailHistory.set(history),
      error: () => this.detailHistory.set([])
    });
  }

  protected onDetailEdit(opportunity: Opportunity): void {
    this.detailVisible.set(false);
    this.openEditDialog(opportunity);
  }

  protected onCustomerFilter(search: string): void {
    this.customerSearch.next(search);
  }

  protected onFormCustomerChange(customerId: string | null): void {
    this.form.controls.customerId.setValue(customerId);
    this.form.controls.contactId.setValue(null);
    this.loadContactOptions(customerId);
  }

  private loadContactOptions(customerId: string | null): void {
    if (!customerId) {
      this.contactOptions.set([]);
      return;
    }
    this.contactService
      .list({ customerId, active: true, size: 100, sort: 'name,asc' })
      .subscribe((response) =>
        this.contactOptions.set(response.content.map((contact) => ({ id: contact.id, name: contact.name })))
      );
  }

  protected onFormPipelineChange(pipelineId: string | null): void {
    this.form.controls.pipelineId.setValue(pipelineId);
    this.form.controls.stageId.setValue(null);
    this.formStages.set(this.stagesOf(pipelineId));
  }

  private stagesOf(pipelineId: string | null): PipelineStage[] {
    if (!pipelineId) {
      return [];
    }
    return this.pipelines().find((pipeline) => pipeline.id === pipelineId)?.stages ?? [];
  }

  protected openCreateDialog(): void {
    const pipelineId = this.selectedPipelineId();
    this.editingOpportunity.set(null);
    this.contactOptions.set([]);
    this.formStages.set(this.stagesOf(pipelineId));
    this.form.reset({
      title: '',
      customerId: null,
      contactId: null,
      pipelineId,
      stageId: null,
      amount: null,
      probability: null,
      ownerUserId: null,
      teamId: null,
      expectedCloseDate: null,
      competitor: '',
      notes: ''
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(opportunity: Opportunity): void {
    this.editingOpportunity.set(opportunity);
    this.ensureCustomerOption(opportunity);
    this.formStages.set(this.stagesOf(opportunity.pipeline?.id ?? null));
    this.loadContactOptions(opportunity.customer?.id ?? null);
    this.form.reset({
      title: opportunity.title,
      customerId: opportunity.customer?.id ?? null,
      contactId: opportunity.contact?.id ?? null,
      pipelineId: opportunity.pipeline?.id ?? null,
      stageId: opportunity.stage?.id ?? null,
      amount: opportunity.amount,
      probability: opportunity.probability,
      ownerUserId: opportunity.owner?.id ?? null,
      teamId: opportunity.team?.id ?? null,
      expectedCloseDate: fromIsoDate(opportunity.expectedCloseDate),
      competitor: opportunity.competitor ?? '',
      notes: opportunity.notes ?? ''
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(opportunity: Opportunity): void {
    const customer = opportunity.customer;
    if (!customer) {
      return;
    }
    const options = this.customerOptions();
    if (options.some((option) => option.id === customer.id)) {
      return;
    }
    this.customerOptions.set([{ id: customer.id, name: customer.name, code: customer.code }, ...options]);
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
    const request: OpportunityRequest = {
      title: raw.title.trim(),
      customerId: raw.customerId as string,
      contactId: raw.contactId,
      pipelineId: raw.pipelineId as string,
      stageId: raw.stageId,
      amount: raw.amount,
      probability: raw.probability,
      ownerUserId: raw.ownerUserId,
      teamId: raw.teamId,
      expectedCloseDate: toIsoDate(raw.expectedCloseDate),
      competitor: trimmedOrNull(raw.competitor),
      notes: trimmedOrNull(raw.notes)
    };

    this.saving.set(true);
    const editing = this.editingOpportunity();
    const request$ = editing
      ? this.opportunityService.update(editing.id, request)
      : this.opportunityService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'commercialPages.opportunities.messages.updated' : 'commercialPages.opportunities.messages.created'
          )
        });
        this.refreshCurrentView();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(opportunity: Opportunity): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: opportunity.title }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.opportunityService.delete(opportunity.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('commercialPages.opportunities.messages.deleted')
          });
          this.refreshCurrentView();
        });
      }
    });
  }

  private refreshCurrentView(): void {
    this.loadBoard();
    if (this.listLoaded) {
      this.loadList();
    }
  }

  protected outcomeSeverity(outcome: OpportunityOutcome): 'success' | 'danger' | 'info' {
    if (outcome === 'WON') {
      return 'success';
    }
    if (outcome === 'LOST') {
      return 'danger';
    }
    return 'info';
  }

  protected chipStyle(color: string | null): Record<string, string> | null {
    return domainChipStyle(color);
  }

  protected currency(value: number | null): string {
    return formatCurrencyBRL(value);
  }

  protected date(value: string | null): string {
    return formatIsoDate(value);
  }

  protected initials(name: string | null | undefined): string {
    return initialsOf(name);
  }
}

function toCustomerOption(customer: Customer): CustomerOption {
  return { id: customer.id, name: customer.name, code: customer.code };
}
