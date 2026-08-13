import {
  ContactSummary,
  CustomerSummary,
  DomainValueSummary,
  LeadSummary,
  OpportunitySummary,
  UserSummary
} from './summary.model';

export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE' | 'CANCELED';

export const TASK_STATUSES: TaskStatus[] = ['PENDING', 'IN_PROGRESS', 'DONE', 'CANCELED'];

export interface Task {
  id: string;
  code: string;
  title: string;
  description: string | null;
  type: DomainValueSummary | null;
  priority: DomainValueSummary | null;
  status: TaskStatus;
  dueAt: string | null;
  reminderAt: string | null;
  completedAt: string | null;
  overdue: boolean;
  assignee: UserSummary | null;
  customer: CustomerSummary | null;
  contact: ContactSummary | null;
  lead: LeadSummary | null;
  opportunity: OpportunitySummary | null;
  resultNotes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TaskRequest {
  title: string;
  description?: string | null;
  typeId?: string | null;
  priorityId?: string | null;
  status?: TaskStatus;
  dueAt?: string | null;
  reminderAt?: string | null;
  assignedUserId?: string | null;
  customerId?: string | null;
  contactId?: string | null;
  leadId?: string | null;
  opportunityId?: string | null;
  resultNotes?: string | null;
}

export interface TaskStatusUpdateRequest {
  status: TaskStatus;
}
