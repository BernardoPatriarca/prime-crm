-- Tabela de usuarios do sistema (autenticacao + preferencias de UI).
CREATE TABLE users (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                     UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    name                          VARCHAR(150) NOT NULL,
    email                         VARCHAR(180) NOT NULL,
    login                         VARCHAR(80) NOT NULL,
    password_hash                 VARCHAR(100) NOT NULL,
    phone                         VARCHAR(30),
    department_domain_value_id    UUID REFERENCES domain_values (id),
    team_domain_value_id          UUID REFERENCES domain_values (id),
    position_domain_value_id      UUID REFERENCES domain_values (id),
    status                        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                      CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    theme_preference              VARCHAR(10) NOT NULL DEFAULT 'LIGHT'
                                      CHECK (theme_preference IN ('LIGHT', 'DARK')),
    language_preference           VARCHAR(10) NOT NULL DEFAULT 'pt-BR',
    timezone                      VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    two_factor_enabled            BOOLEAN NOT NULL DEFAULT false,
    last_login_at                 TIMESTAMPTZ,
    avatar_url                    VARCHAR(500),
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                    VARCHAR(120),
    updated_by                    VARCHAR(120),
    deleted_at                    TIMESTAMPTZ
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_department_domain_value_id ON users (department_domain_value_id);
CREATE INDEX idx_users_team_domain_value_id ON users (team_domain_value_id);
CREATE INDEX idx_users_position_domain_value_id ON users (position_domain_value_id);
CREATE INDEX idx_users_not_deleted ON users (id) WHERE deleted_at IS NULL;

-- email/login unicos globalmente, mas ignorando registros soft-deleted (permite reuso apos exclusao logica).
CREATE UNIQUE INDEX uq_users_email ON users (email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_login ON users (login) WHERE deleted_at IS NULL;
