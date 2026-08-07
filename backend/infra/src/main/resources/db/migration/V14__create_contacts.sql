-- Fase 2 - Nucleo Comercial: contatos (pessoas) vinculados a um cliente.
-- O cargo (position_title) e texto livre; o departamento reaproveita domain_values (tipo DEPARTMENT).
CREATE TABLE contacts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    customer_id      UUID NOT NULL REFERENCES customers (id),
    name             VARCHAR(200) NOT NULL,
    position_title   VARCHAR(120),
    department_id    UUID REFERENCES domain_values (id),
    email            VARCHAR(180),
    phone            VARCHAR(30),
    mobile           VARCHAR(30),
    birth_date       DATE,
    linkedin         VARCHAR(255),
    primary_contact  BOOLEAN NOT NULL DEFAULT false,
    decision_maker   BOOLEAN NOT NULL DEFAULT false,
    notes            TEXT,
    active           BOOLEAN NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by       VARCHAR(120),
    updated_by       VARCHAR(120),
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX idx_contacts_tenant_id ON contacts (tenant_id);
CREATE INDEX idx_contacts_customer_id ON contacts (customer_id);
CREATE INDEX idx_contacts_department_id ON contacts (department_id);
CREATE INDEX idx_contacts_not_deleted ON contacts (id) WHERE deleted_at IS NULL;

-- Carregamento em lote dos contatos de uma pagina de clientes.
CREATE INDEX idx_contacts_customer_not_deleted ON contacts (customer_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contacts_tenant_name ON contacts (tenant_id, name) WHERE deleted_at IS NULL;
CREATE INDEX idx_contacts_email ON contacts (email) WHERE email IS NOT NULL AND deleted_at IS NULL;
