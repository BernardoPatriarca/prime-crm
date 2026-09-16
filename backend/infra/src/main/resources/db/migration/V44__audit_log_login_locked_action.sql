ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS ck_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT ck_audit_log_action
    CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGIN_FAILED', 'LOGIN_LOCKED', 'LOGOUT',
                      'PASSWORD_CHANGED', 'EXPORT'));
