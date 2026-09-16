CREATE INDEX idx_tasks_assignee_status_due_at
    ON tasks (assigned_user_id, status, due_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_opportunities_owner_outcome_expected_close_date
    ON opportunities (owner_user_id, outcome, expected_close_date) WHERE deleted_at IS NULL;

CREATE INDEX idx_receivables_paid_at
    ON receivables (paid_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_payables_paid_at
    ON payables (paid_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_calendar_events_assignee_status
    ON calendar_events (assigned_user_id, status) WHERE deleted_at IS NULL;
