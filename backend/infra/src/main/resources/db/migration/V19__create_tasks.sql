-- Fase 3 - Tarefas: atividades de follow-up da operacao comercial. A tarefa e
-- generica e pode estar ligada (opcionalmente) a cliente, contato, lead ou oportunidade.
-- Tipo e prioridade reaproveitam o engine domain_values (TASK_TYPE / PRIORITY).
CREATE SEQUENCE task_code_seq START 1000;

CREATE TABLE tasks (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code              VARCHAR(30) DEFAULT ('TAR-' || lpad(nextval('task_code_seq')::text, 6, '0')),
    title             VARCHAR(200) NOT NULL,
    description       TEXT,
    type_id           UUID REFERENCES domain_values (id),
    priority_id       UUID REFERENCES domain_values (id),
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE', 'CANCELED')),
    due_at            TIMESTAMPTZ,
    reminder_at       TIMESTAMPTZ,
    completed_at      TIMESTAMPTZ,
    assigned_user_id  UUID REFERENCES users (id),
    customer_id       UUID REFERENCES customers (id),
    contact_id        UUID REFERENCES contacts (id),
    lead_id           UUID REFERENCES leads (id),
    opportunity_id    UUID REFERENCES opportunities (id),
    result_notes      TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(120),
    updated_by        VARCHAR(120),
    deleted_at        TIMESTAMPTZ
);

CREATE INDEX idx_tasks_tenant_id ON tasks (tenant_id);
CREATE INDEX idx_tasks_type_id ON tasks (type_id);
CREATE INDEX idx_tasks_priority_id ON tasks (priority_id);
CREATE INDEX idx_tasks_assigned_user_id ON tasks (assigned_user_id);
CREATE INDEX idx_tasks_customer_id ON tasks (customer_id);
CREATE INDEX idx_tasks_contact_id ON tasks (contact_id);
CREATE INDEX idx_tasks_lead_id ON tasks (lead_id);
CREATE INDEX idx_tasks_opportunity_id ON tasks (opportunity_id);
CREATE INDEX idx_tasks_not_deleted ON tasks (id) WHERE deleted_at IS NULL;

-- Colunas usadas em ordenacao, filtro e agrupamento de relatorio das listagens.
CREATE INDEX idx_tasks_tenant_title ON tasks (tenant_id, title) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_tenant_status ON tasks (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_due_at ON tasks (due_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_tasks_completed_at ON tasks (completed_at) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_tasks_tenant_code
    ON tasks (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;
