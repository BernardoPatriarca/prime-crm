export type ReportKey = 'customers' | 'opportunities' | 'tasks';

export type CustomerReportGroupBy =
  | 'CLIENT_TYPE'
  | 'SEGMENT'
  | 'ACTIVITY_BRANCH'
  | 'CATEGORY'
  | 'ORIGIN'
  | 'STATUS'
  | 'TEAM'
  | 'OWNER'
  | 'PERSON_TYPE'
  | 'STATE'
  | 'CITY'
  | 'ACTIVE'
  | 'CREATED_MONTH';

export type OpportunityReportGroupBy =
  | 'PIPELINE'
  | 'STAGE'
  | 'OUTCOME'
  | 'OWNER'
  | 'TEAM'
  | 'CUSTOMER'
  | 'WIN_REASON'
  | 'LOSS_REASON'
  | 'COMPETITOR'
  | 'OPENED_MONTH'
  | 'EXPECTED_CLOSE_MONTH'
  | 'CLOSED_MONTH';

export type TaskReportGroupBy =
  | 'STATUS'
  | 'TYPE'
  | 'PRIORITY'
  | 'ASSIGNEE'
  | 'CUSTOMER'
  | 'OPPORTUNITY'
  | 'DUE_MONTH'
  | 'CREATED_MONTH'
  | 'COMPLETED_MONTH'
  | 'OVERDUE';

export type ReportGroupBy = CustomerReportGroupBy | OpportunityReportGroupBy | TaskReportGroupBy;

export const REPORT_GROUP_BY_OPTIONS: Record<ReportKey, ReportGroupBy[]> = {
  customers: [
    'CLIENT_TYPE',
    'SEGMENT',
    'ACTIVITY_BRANCH',
    'CATEGORY',
    'ORIGIN',
    'STATUS',
    'TEAM',
    'OWNER',
    'PERSON_TYPE',
    'STATE',
    'CITY',
    'ACTIVE',
    'CREATED_MONTH'
  ],
  opportunities: [
    'STAGE',
    'PIPELINE',
    'OUTCOME',
    'OWNER',
    'TEAM',
    'CUSTOMER',
    'WIN_REASON',
    'LOSS_REASON',
    'COMPETITOR',
    'OPENED_MONTH',
    'EXPECTED_CLOSE_MONTH',
    'CLOSED_MONTH'
  ],
  tasks: [
    'STATUS',
    'PRIORITY',
    'TYPE',
    'ASSIGNEE',
    'CUSTOMER',
    'OPPORTUNITY',
    'OVERDUE',
    'DUE_MONTH',
    'CREATED_MONTH',
    'COMPLETED_MONTH'
  ]
};

export interface ReportGroupRow {
  label: string | null;
  count: number;
  total: number | null;
  percentage: number;
}

export interface Report {
  report: string;
  groupBy: ReportGroupBy;
  measured: boolean;
  totalCount: number;
  totalAmount: number | null;
  generatedAt: string;
  rows: ReportGroupRow[];
}

export interface ReportQuery {
  groupBy: ReportGroupBy;
  from?: string | null;
  to?: string | null;
  userId?: string | null;
}
