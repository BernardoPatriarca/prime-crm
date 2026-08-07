-- Feriados nacionais/regionais usados no calculo de SLA e agenda.
CREATE TABLE holidays (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    holiday_date   DATE NOT NULL,
    name           VARCHAR(150) NOT NULL,
    national       BOOLEAN NOT NULL DEFAULT true,
    active         BOOLEAN NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(120),
    updated_by     VARCHAR(120),
    deleted_at     TIMESTAMPTZ
);

CREATE INDEX idx_holidays_tenant_id ON holidays (tenant_id);
CREATE INDEX idx_holidays_holiday_date ON holidays (holiday_date);
CREATE INDEX idx_holidays_not_deleted ON holidays (id) WHERE deleted_at IS NULL;
