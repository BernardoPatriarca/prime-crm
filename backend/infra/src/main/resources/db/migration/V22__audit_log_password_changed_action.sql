-- Troca de senha pelo proprio usuario passa a ser um evento auditado de sessao.
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS ck_audit_log_action;

ALTER TABLE audit_log ADD CONSTRAINT ck_audit_log_action
    CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGIN_FAILED', 'LOGOUT',
                      'PASSWORD_CHANGED', 'EXPORT'));
