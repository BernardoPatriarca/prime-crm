export type NotificationType =
  | 'TASK_OVERDUE'
  | 'TASK_DUE_TODAY'
  | 'OPPORTUNITY_CLOSE_DATE_PASSED'
  | 'LEAD_WITHOUT_OWNER';

export type NotificationSeverity = 'DANGER' | 'WARN' | 'INFO';

export interface AppNotification {
  type: NotificationType;
  severity: NotificationSeverity;
  referenceId: string;
  title: string;
  description: string | null;
  link: string;
  date: string | null;
}

export interface NotificationList {
  total: number;
  items: AppNotification[];
}
