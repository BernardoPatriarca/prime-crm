import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { CalendarEvent } from '../../core/models/calendar-event.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { CalendarEventService } from '../../core/services/calendar-event.service';
import { CustomerService } from '../../core/services/customer.service';
import { DomainValueService } from '../../core/services/domain-value.service';
import { AgendaPageComponent } from './agenda-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function eventFixture(overrides: Partial<CalendarEvent> = {}): CalendarEvent {
  return {
    id: 'event-1',
    title: 'Reuniao com cliente',
    description: null,
    type: null,
    status: 'SCHEDULED',
    startAt: '2026-02-01T12:00:00Z',
    endAt: null,
    allDay: false,
    location: null,
    reminderAt: null,
    overdue: false,
    assignee: null,
    customer: null,
    contact: null,
    lead: null,
    opportunity: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('AgendaPageComponent', () => {
  let fixture: ComponentFixture<AgendaPageComponent>;
  let component: AgendaPageComponent;
  let calendarEventServiceStub: jasmine.SpyObj<CalendarEventService>;

  beforeEach(async () => {
    localStorage.clear();

    calendarEventServiceStub = jasmine.createSpyObj<CalendarEventService>('CalendarEventService', [
      'list',
      'range',
      'create',
      'update',
      'delete'
    ]);
    calendarEventServiceStub.list.and.returnValue(of(emptyPage));
    calendarEventServiceStub.range.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [AgendaPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: CalendarEventService, useValue: calendarEventServiceStub },
        { provide: CustomerService, useValue: { list: () => of(emptyPage) } },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AgendaPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads the month range on init', () => {
    expect(calendarEventServiceStub.range).toHaveBeenCalled();
  });

  it('switches to the list view and loads paginated events', () => {
    component['onViewModeChange']('list');

    expect(component['viewMode']()).toBe('list');
    expect(calendarEventServiceStub.list).toHaveBeenCalled();
  });

  it('requires a title and a start date before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.title.invalid).toBeTrue();
    expect(component['form'].controls.startAt.invalid).toBeTrue();
    expect(calendarEventServiceStub.create).not.toHaveBeenCalled();
  });

  it('creates an event with the scheduled status by default', () => {
    expect(component['form'].controls.status.value).toBe('SCHEDULED');
  });

  it('loads the linked values of an event when editing', () => {
    component['openEditDialog'](eventFixture({ title: 'Ligar para o lead', status: 'DONE' }));

    expect(component['form'].controls.title.value).toBe('Ligar para o lead');
    expect(component['form'].controls.status.value).toBe('DONE');
    expect(component['form'].controls.startAt.value).toBeInstanceOf(Date);
  });

  it('navigates between months and reloads the range', () => {
    const initialMonth = component['currentMonth']();
    calendarEventServiceStub.range.calls.reset();

    component['nextMonth']();

    expect(component['currentMonth']().getMonth()).not.toBe(initialMonth.getMonth());
    expect(calendarEventServiceStub.range).toHaveBeenCalled();
  });
});
