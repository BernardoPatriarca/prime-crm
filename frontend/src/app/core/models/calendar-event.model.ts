import {
  ContactSummary,
  CustomerSummary,
  DomainValueSummary,
  LeadSummary,
  OpportunitySummary,
  UserSummary
} from './summary.model';

export type CalendarEventStatus = 'SCHEDULED' | 'DONE' | 'CANCELED';

export const CALENDAR_EVENT_STATUSES: CalendarEventStatus[] = ['SCHEDULED', 'DONE', 'CANCELED'];

export interface CalendarEvent {
  id: string;
  title: string;
  description: string | null;
  type: DomainValueSummary | null;
  status: CalendarEventStatus;
  startAt: string;
  endAt: string | null;
  allDay: boolean;
  location: string | null;
  reminderAt: string | null;
  overdue: boolean;
  assignee: UserSummary | null;
  customer: CustomerSummary | null;
  contact: ContactSummary | null;
  lead: LeadSummary | null;
  opportunity: OpportunitySummary | null;
  createdAt: string;
  updatedAt: string;
}

export interface CalendarEventRequest {
  title: string;
  description?: string | null;
  typeId?: string | null;
  status?: CalendarEventStatus;
  startAt: string;
  endAt?: string | null;
  allDay: boolean;
  location?: string | null;
  reminderAt?: string | null;
  assignedUserId?: string | null;
  customerId?: string | null;
  contactId?: string | null;
  leadId?: string | null;
  opportunityId?: string | null;
}
