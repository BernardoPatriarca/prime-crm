import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Task } from '../../core/models/task.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { CustomerService } from '../../core/services/customer.service';
import { DomainValueService } from '../../core/services/domain-value.service';
import { TaskService } from '../../core/services/task.service';
import { TasksPageComponent } from './tasks-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function taskFixture(overrides: Partial<Task> = {}): Task {
  return {
    id: 'task-1',
    code: 'TAR-001000',
    title: 'Ligar para o cliente',
    description: null,
    type: null,
    priority: null,
    status: 'PENDING',
    dueAt: '2026-02-01T12:00:00Z',
    reminderAt: null,
    completedAt: null,
    overdue: false,
    assignee: null,
    customer: null,
    contact: null,
    lead: null,
    opportunity: null,
    resultNotes: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('TasksPageComponent', () => {
  let fixture: ComponentFixture<TasksPageComponent>;
  let component: TasksPageComponent;
  let taskServiceStub: jasmine.SpyObj<TaskService>;

  beforeEach(async () => {
    localStorage.clear();

    taskServiceStub = jasmine.createSpyObj<TaskService>('TaskService', ['list', 'create', 'update', 'changeStatus', 'delete']);
    taskServiceStub.list.and.returnValue(of(emptyPage));
    taskServiceStub.changeStatus.and.returnValue(of(taskFixture({ status: 'DONE' })));

    await TestBed.configureTestingModule({
      imports: [TasksPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: TaskService, useValue: taskServiceStub },
        { provide: CustomerService, useValue: { list: () => of(emptyPage) } },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TasksPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no tasks', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a title before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.title.invalid).toBeTrue();
    expect(taskServiceStub.create).not.toHaveBeenCalled();
  });

  it('creates a task with the pending status by default', () => {
    expect(component['form'].controls.status.value).toBe('PENDING');
  });

  it('sends the overdue filter to the API only when it is selected', () => {
    component['overdueFilter'].set(true);
    component['onFilterChange']();

    expect(taskServiceStub.list.calls.mostRecent().args[0].overdue).toBeTrue();

    component['overdueFilter'].set(null);
    component['onFilterChange']();

    expect(taskServiceStub.list.calls.mostRecent().args[0].overdue).toBeUndefined();
  });

  it('completes a task through the status endpoint', () => {
    component['complete'](taskFixture());

    expect(taskServiceStub.changeStatus).toHaveBeenCalledWith('task-1', { status: 'DONE' });
  });

  it('loads the linked values of a task when editing', () => {
    component['openEditDialog'](taskFixture({ title: 'Enviar proposta', status: 'IN_PROGRESS' }));

    expect(component['form'].controls.title.value).toBe('Enviar proposta');
    expect(component['form'].controls.status.value).toBe('IN_PROGRESS');
    expect(component['form'].controls.dueAt.value).toBeInstanceOf(Date);
  });
});
