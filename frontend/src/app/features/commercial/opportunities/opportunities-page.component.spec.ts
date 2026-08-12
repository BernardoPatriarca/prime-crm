import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of, throwError } from 'rxjs';
import { OpportunityBoard, OpportunityCard } from '../../../core/models/opportunity.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { ContactService } from '../../../core/services/contact.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { OpportunityService } from '../../../core/services/opportunity.service';
import { PipelineService } from '../../../core/services/pipeline.service';
import { SessionStore } from '../../../core/store/session.store';
import { OpportunitiesPageComponent } from './opportunities-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function pageOf<T>(content: T[]) {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages: 1, last: true };
}

function buildCard(id: string, amount: number): OpportunityCard {
  return {
    id,
    code: `OPO-${id}`,
    title: `Negócio ${id}`,
    amount,
    probability: 20,
    expectedCloseDate: '2026-10-15',
    openedAt: '2026-08-01T12:00:00Z',
    customer: { id: 'cus-1', code: 'CLI-000001', name: 'Cliente Teste' },
    owner: { id: 'usr-1', name: 'Ana Souza', email: 'ana@teste.com' }
  };
}

function buildBoard(): OpportunityBoard {
  return {
    pipelineId: 'pipe-1',
    pipelineName: 'Funil Padrão',
    limitPerStage: 50,
    totalCount: 3,
    totalAmount: 6000,
    columns: [
      {
        stageId: 'stage-1',
        stageName: 'Qualificação',
        displayOrder: 1,
        defaultProbability: 20,
        color: '#1e5eff',
        requiresLossReason: false,
        totalCount: 2,
        totalAmount: 3000,
        hasMore: false,
        opportunities: [buildCard('opp-1', 1000), buildCard('opp-2', 2000)]
      },
      {
        stageId: 'stage-2',
        stageName: 'Proposta',
        displayOrder: 2,
        defaultProbability: 60,
        color: '#f59e0b',
        requiresLossReason: false,
        totalCount: 1,
        totalAmount: 3000,
        hasMore: false,
        opportunities: [buildCard('opp-3', 3000)]
      },
      {
        stageId: 'stage-lost',
        stageName: 'Perdido',
        displayOrder: 3,
        defaultProbability: 0,
        color: '#ef4444',
        requiresLossReason: true,
        totalCount: 0,
        totalAmount: 0,
        hasMore: false,
        opportunities: []
      },
      {
        stageId: 'stage-won',
        stageName: 'Ganho',
        displayOrder: 4,
        defaultProbability: 100,
        color: '#22c55e',
        requiresLossReason: false,
        totalCount: 0,
        totalAmount: 0,
        hasMore: false,
        opportunities: []
      }
    ]
  };
}

const pipeline = {
  id: 'pipe-1',
  name: 'Funil Padrão',
  businessType: null,
  active: true,
  stages: [
    {
      id: 'stage-1',
      pipelineId: 'pipe-1',
      name: 'Qualificação',
      displayOrder: 1,
      defaultProbability: 20,
      slaDays: null,
      color: '#1e5eff',
      requiresLossReason: false
    }
  ]
};

describe('OpportunitiesPageComponent', () => {
  let fixture: ComponentFixture<OpportunitiesPageComponent>;
  let component: OpportunitiesPageComponent;
  let opportunityServiceStub: {
    list: jasmine.Spy;
    board: jasmine.Spy;
    getById: jasmine.Spy;
    history: jasmine.Spy;
    moveStage: jasmine.Spy;
    create: jasmine.Spy;
    update: jasmine.Spy;
    delete: jasmine.Spy;
  };

  function columnById(stageId: string) {
    return component['boardColumns']().find((column) => column.stageId === stageId)!;
  }

  function cardIdsOf(stageId: string): string[] {
    return columnById(stageId).cards.map((card) => card.id);
  }

  beforeEach(async () => {
    localStorage.clear();

    opportunityServiceStub = {
      list: jasmine.createSpy('list').and.returnValue(of(emptyPage)),
      board: jasmine.createSpy('board').and.callFake(() => of(buildBoard())),
      getById: jasmine.createSpy('getById').and.returnValue(of({ id: 'opp-1', title: 'Negócio opp-1' })),
      history: jasmine.createSpy('history').and.returnValue(of([])),
      moveStage: jasmine.createSpy('moveStage').and.returnValue(of({ id: 'opp-1' })),
      create: jasmine.createSpy('create').and.returnValue(of({ id: 'opp-new' })),
      update: jasmine.createSpy('update').and.returnValue(of({ id: 'opp-1' })),
      delete: jasmine.createSpy('delete').and.returnValue(of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [OpportunitiesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: OpportunityService, useValue: opportunityServiceStub },
        { provide: PipelineService, useValue: { list: () => of(pageOf([pipeline])) } },
        { provide: CustomerService, useValue: { list: () => of(emptyPage) } },
        { provide: ContactService, useValue: { list: () => of(emptyPage) } },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } },
        { provide: SessionStore, useValue: { hasPermission: () => true } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OpportunitiesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('selects the first active pipeline and loads its board', () => {
    expect(component['selectedPipelineId']()).toBe('pipe-1');
    expect(opportunityServiceStub.board).toHaveBeenCalledWith('pipe-1', 50);
  });

  it('groups the board opportunities per stage in display order', () => {
    expect(component['boardColumns']().map((column) => column.stageId)).toEqual([
      'stage-1',
      'stage-2',
      'stage-lost',
      'stage-won'
    ]);
    expect(cardIdsOf('stage-1')).toEqual(['opp-1', 'opp-2']);
    expect(cardIdsOf('stage-2')).toEqual(['opp-3']);
  });

  it('exposes the board totalizers', () => {
    expect(component['boardCount']()).toBe(3);
    expect(component['boardAmount']()).toBe(6000);
  });

  it('keeps a single table mounted while loading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    component['loading'].set(true);
    fixture.detectChanges();

    expect(compiled.querySelectorAll('[data-testid="generic-table"]').length).toBe(1);
  });

  it('keeps the table mounted while the board view is active', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(component['view']()).toBe('BOARD');
    expect(compiled.querySelectorAll('[data-testid="generic-table"]').length).toBe(1);
  });

  it('only queries the list the first time the list view is opened', () => {
    expect(opportunityServiceStub.list).not.toHaveBeenCalled();

    component['onViewChange']('LIST');
    expect(opportunityServiceStub.list).toHaveBeenCalledTimes(1);

    component['onViewChange']('BOARD');
    component['onViewChange']('LIST');
    expect(opportunityServiceStub.list).toHaveBeenCalledTimes(1);
  });

  it('moves the card optimistically and patches the stage on a regular target', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-2',
      targetIndex: 0
    });

    expect(opportunityServiceStub.moveStage).toHaveBeenCalledWith('opp-1', { stageId: 'stage-2' });
  });

  it('reverts the card to the source column when the stage patch fails', () => {
    opportunityServiceStub.moveStage.and.returnValue(throwError(() => new Error('boom')));

    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-2',
      targetIndex: 0
    });

    expect(cardIdsOf('stage-1')).toEqual(['opp-1', 'opp-2']);
    expect(cardIdsOf('stage-2')).toEqual(['opp-3']);
    expect(columnById('stage-1').totalCount).toBe(2);
    expect(columnById('stage-1').totalAmount).toBe(3000);
    expect(columnById('stage-2').totalCount).toBe(1);
    expect(columnById('stage-2').totalAmount).toBe(3000);
    expect(component['movingCardId']()).toBeNull();
  });

  it('reverts the card to its original position when the patch fails from the middle of a column', () => {
    opportunityServiceStub.moveStage.and.returnValue(throwError(() => new Error('boom')));

    component['onCardDropped']({
      cardId: 'opp-2',
      fromStageId: 'stage-1',
      toStageId: 'stage-2',
      targetIndex: 0
    });

    expect(cardIdsOf('stage-1')).toEqual(['opp-1', 'opp-2']);
  });

  it('does not reload the board when the move fails', () => {
    opportunityServiceStub.board.calls.reset();
    opportunityServiceStub.moveStage.and.returnValue(throwError(() => new Error('boom')));

    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-2',
      targetIndex: 0
    });

    expect(opportunityServiceStub.board).not.toHaveBeenCalled();
  });

  it('asks for the loss reason before patching a stage that requires it', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-lost',
      targetIndex: 0
    });

    expect(component['reasonDialogVisible']()).toBeTrue();
    expect(component['reasonRequirement']()).toBe('LOSS');
    expect(opportunityServiceStub.moveStage).not.toHaveBeenCalled();
    expect(cardIdsOf('stage-lost')).toEqual(['opp-1']);
  });

  it('asks for the win reason on a hundred percent stage', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-won',
      targetIndex: 0
    });

    expect(component['reasonRequirement']()).toBe('WIN');
    expect(opportunityServiceStub.moveStage).not.toHaveBeenCalled();
  });

  it('sends the loss reason when the dialog is confirmed', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-lost',
      targetIndex: 0
    });
    component['reasonForm'].controls.reasonId.setValue('reason-1');
    component['reasonForm'].controls.note.setValue('  cliente desistiu  ');

    component['confirmReason']();

    expect(opportunityServiceStub.moveStage).toHaveBeenCalledWith('opp-1', {
      stageId: 'stage-lost',
      lossReasonId: 'reason-1',
      note: 'cliente desistiu'
    });
    expect(component['reasonDialogVisible']()).toBeFalse();
  });

  it('sends the win reason when the dialog is confirmed', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-won',
      targetIndex: 0
    });
    component['reasonForm'].controls.reasonId.setValue('reason-win');

    component['confirmReason']();

    expect(opportunityServiceStub.moveStage).toHaveBeenCalledWith('opp-1', {
      stageId: 'stage-won',
      winReasonId: 'reason-win',
      note: null
    });
  });

  it('blocks the confirmation while no reason is chosen', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-lost',
      targetIndex: 0
    });

    component['confirmReason']();

    expect(opportunityServiceStub.moveStage).not.toHaveBeenCalled();
    expect(component['reasonForm'].controls.reasonId.hasError('required')).toBeTrue();
    expect(component['reasonDialogVisible']()).toBeTrue();
  });

  it('returns the card to the source column when the reason dialog is cancelled', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-lost',
      targetIndex: 0
    });

    component['cancelReason']();

    expect(cardIdsOf('stage-1')).toEqual(['opp-1', 'opp-2']);
    expect(cardIdsOf('stage-lost')).toEqual([]);
    expect(columnById('stage-1').totalAmount).toBe(3000);
    expect(columnById('stage-lost').totalCount).toBe(0);
    expect(opportunityServiceStub.moveStage).not.toHaveBeenCalled();
  });

  it('does not revert twice when the dialog hide fires after a cancel', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-lost',
      targetIndex: 0
    });

    component['cancelReason']();
    component['discardPendingMove']();

    expect(cardIdsOf('stage-1')).toEqual(['opp-1', 'opp-2']);
    expect(columnById('stage-1').totalCount).toBe(2);
  });

  it('does not revert after the reason dialog was confirmed', () => {
    component['onCardDropped']({
      cardId: 'opp-1',
      fromStageId: 'stage-1',
      toStageId: 'stage-lost',
      targetIndex: 0
    });
    component['reasonForm'].controls.reasonId.setValue('reason-1');
    component['confirmReason']();

    component['discardPendingMove']();

    expect(component['pendingMove']()).toBeNull();
  });

  it('ignores a drop whose source column no longer holds the card', () => {
    component['onCardDropped']({
      cardId: 'ghost',
      fromStageId: 'stage-1',
      toStageId: 'stage-2',
      targetIndex: 0
    });

    expect(opportunityServiceStub.moveStage).not.toHaveBeenCalled();
  });

  it('reloads the board when the pipeline changes', () => {
    opportunityServiceStub.board.calls.reset();

    component['onPipelineChange']('pipe-1');

    expect(opportunityServiceStub.board).toHaveBeenCalledWith('pipe-1', 50);
  });

  it('requires a title and a customer before saving', () => {
    component['openCreateDialog']();

    component['save']();

    expect(opportunityServiceStub.create).not.toHaveBeenCalled();
    expect(component['form'].controls.title.invalid).toBeTrue();
    expect(component['form'].controls.customerId.invalid).toBeTrue();
  });

  it('prefills the create dialog with the selected pipeline', () => {
    component['openCreateDialog']();

    expect(component['form'].controls.pipelineId.value).toBe('pipe-1');
    expect(component['formStages']().length).toBe(1);
  });

  it('loads the detail and the history when a card is selected', () => {
    component['onCardSelected'](buildCard('opp-1', 1000));

    expect(component['detailVisible']()).toBeTrue();
    expect(opportunityServiceStub.getById).toHaveBeenCalledWith('opp-1');
    expect(opportunityServiceStub.history).toHaveBeenCalledWith('opp-1');
  });
});
