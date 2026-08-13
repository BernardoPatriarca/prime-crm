-- Fase 3 - Auditoria completa: alem de CREATE/UPDATE/DELETE o log passa a registrar
-- eventos de sessao (LOGIN, LOGIN_FAILED, LOGOUT) e extracoes de dados (EXPORT).
-- O CHECK original foi criado sem nome explicito pelo Postgres (audit_log_action_check).
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_action_check;

ALTER TABLE audit_log ADD CONSTRAINT ck_audit_log_action
    CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGIN_FAILED', 'LOGOUT', 'EXPORT'));

-- Indices de apoio a tela de auditoria (filtros por usuario, acao e periodo).
CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
CREATE INDEX idx_audit_log_tenant_created_at ON audit_log (tenant_id, created_at DESC);
