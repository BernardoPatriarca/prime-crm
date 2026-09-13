-- Fase 3 - Agenda: compromissos (reunioes, ligacoes, visitas) do usuario, ligados
-- opcionalmente a cliente, contato, lead ou oportunidade. Tipo reaproveita o mesmo
-- engine domain_values ja usado por Tarefas (TASK_TYPE) em vez de um catalogo proprio.
CREATE TABLE calendar_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    type_id           UUID REFERENCES domain_values (id),
    status            VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                          CHECK (status IN ('SCHEDULED', 'DONE', 'CANCELED')),
    start_at          TIMESTAMPTZ NOT NULL,
    end_at            TIMESTAMPTZ,
    all_day           BOOLEAN NOT NULL DEFAULT false,
    location          VARCHAR(200),
    reminder_at       TIMESTAMPTZ,
    assigned_user_id  UUID REFERENCES users (id),
    customer_id       UUID REFERENCES customers (id),
    contact_id        UUID REFERENCES contacts (id),
    lead_id           UUID REFERENCES leads (id),
    opportunity_id    UUID REFERENCES opportunities (id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(120),
    updated_by        VARCHAR(120),
    deleted_at        TIMESTAMPTZ,
    CHECK (end_at IS NULL OR end_at >= start_at)
);

CREATE INDEX idx_calendar_events_tenant_id ON calendar_events (tenant_id);
CREATE INDEX idx_calendar_events_type_id ON calendar_events (type_id);
CREATE INDEX idx_calendar_events_assigned_user_id ON calendar_events (assigned_user_id);
CREATE INDEX idx_calendar_events_customer_id ON calendar_events (customer_id);
CREATE INDEX idx_calendar_events_contact_id ON calendar_events (contact_id);
CREATE INDEX idx_calendar_events_lead_id ON calendar_events (lead_id);
CREATE INDEX idx_calendar_events_opportunity_id ON calendar_events (opportunity_id);
CREATE INDEX idx_calendar_events_not_deleted ON calendar_events (id) WHERE deleted_at IS NULL;

-- Range start_at/end_at e o filtro mais comum da tela (mes/semana/dia visivel).
CREATE INDEX idx_calendar_events_range ON calendar_events (start_at, end_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_calendar_events_tenant_status ON calendar_events (tenant_id, status) WHERE deleted_at IS NULL;
