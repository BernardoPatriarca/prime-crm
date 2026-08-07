-- Fase 2 - Nucleo Comercial: cadastro unico de clientes (pessoa fisica e juridica).
-- Decisao de modelagem: nao existe tabela separada de "empresas". A tela Empresas e este
-- mesmo cadastro filtrado por person_type = 'JURIDICA'.
-- Grupo economico (matriz/filial) e resolvido por auto-referencia em parent_customer_id.
CREATE TABLE customers (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code                     VARCHAR(30),
    name                     VARCHAR(200) NOT NULL,
    trade_name               VARCHAR(200),
    person_type              VARCHAR(10) NOT NULL DEFAULT 'JURIDICA'
                                 CHECK (person_type IN ('FISICA', 'JURIDICA')),
    -- CPF ou CNPJ armazenado somente com digitos (sem mascara).
    document                 VARCHAR(20),
    state_registration       VARCHAR(30),
    municipal_registration   VARCHAR(30),
    client_type_id           UUID REFERENCES domain_values (id),
    segment_id               UUID REFERENCES domain_values (id),
    activity_branch_id       UUID REFERENCES domain_values (id),
    category_id              UUID REFERENCES domain_values (id),
    origin_id                UUID REFERENCES domain_values (id),
    status_id                UUID REFERENCES domain_values (id),
    owner_user_id            UUID REFERENCES users (id),
    team_id                  UUID REFERENCES domain_values (id),
    phone                    VARCHAR(30),
    mobile                   VARCHAR(30),
    email                    VARCHAR(180),
    financial_email          VARCHAR(180),
    website                  VARCHAR(255),
    instagram                VARCHAR(120),
    linkedin                 VARCHAR(255),
    zip_code                 VARCHAR(20),
    street                   VARCHAR(200),
    number                   VARCHAR(20),
    complement               VARCHAR(120),
    district                 VARCHAR(120),
    city                     VARCHAR(120),
    state                    VARCHAR(2),
    country                  VARCHAR(60) NOT NULL DEFAULT 'Brasil',
    latitude                 NUMERIC(10,7),
    longitude                NUMERIC(10,7),
    -- Data de nascimento (pessoa fisica) ou data de fundacao (pessoa juridica).
    birth_date               DATE,
    last_contact_at          TIMESTAMPTZ,
    next_contact_at          TIMESTAMPTZ,
    credit_limit             NUMERIC(15,2),
    payment_terms            VARCHAR(120),
    health_score             INT CHECK (health_score >= 0 AND health_score <= 100),
    notes                    TEXT,
    parent_customer_id       UUID REFERENCES customers (id),
    active                   BOOLEAN NOT NULL DEFAULT true,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by               VARCHAR(120),
    updated_by               VARCHAR(120),
    deleted_at               TIMESTAMPTZ
);

CREATE INDEX idx_customers_tenant_id ON customers (tenant_id);
CREATE INDEX idx_customers_client_type_id ON customers (client_type_id);
CREATE INDEX idx_customers_segment_id ON customers (segment_id);
CREATE INDEX idx_customers_activity_branch_id ON customers (activity_branch_id);
CREATE INDEX idx_customers_category_id ON customers (category_id);
CREATE INDEX idx_customers_origin_id ON customers (origin_id);
CREATE INDEX idx_customers_status_id ON customers (status_id);
CREATE INDEX idx_customers_owner_user_id ON customers (owner_user_id);
CREATE INDEX idx_customers_team_id ON customers (team_id);
CREATE INDEX idx_customers_parent_customer_id ON customers (parent_customer_id);
CREATE INDEX idx_customers_not_deleted ON customers (id) WHERE deleted_at IS NULL;

-- Colunas usadas em ordenacao/filtro/busca das listagens.
CREATE INDEX idx_customers_tenant_name ON customers (tenant_id, name) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_tenant_person_type ON customers (tenant_id, person_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_tenant_active ON customers (tenant_id, active) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_document ON customers (document) WHERE document IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_customers_next_contact_at ON customers (next_contact_at) WHERE deleted_at IS NULL;

-- Codigo legivel e documento (CPF/CNPJ) unicos por tenant, ignorando registros soft-deleted.
CREATE UNIQUE INDEX uq_customers_tenant_code
    ON customers (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_customers_tenant_document
    ON customers (tenant_id, document)
    WHERE document IS NOT NULL AND deleted_at IS NULL;

-- Vinculo N:N entre cliente e domain_values do tipo TAG.
-- Tabela de ligacao pura: sem tenant_id, sem auditoria completa e sem soft delete.
CREATE TABLE customer_tags (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      UUID NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    domain_value_id  UUID NOT NULL REFERENCES domain_values (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_customer_tags UNIQUE (customer_id, domain_value_id)
);

CREATE INDEX idx_customer_tags_customer_id ON customer_tags (customer_id);
CREATE INDEX idx_customer_tags_domain_value_id ON customer_tags (domain_value_id);
