-- RBAC: perfis (roles) por tenant, catalogo tecnico fixo de permissoes,
-- e as tabelas de associacao role_permissions / user_roles.

CREATE TABLE roles (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(255),
    active       BOOLEAN NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   VARCHAR(120),
    updated_by   VARCHAR(120),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX idx_roles_tenant_id ON roles (tenant_id);
CREATE INDEX idx_roles_not_deleted ON roles (id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_roles_tenant_name ON roles (tenant_id, name) WHERE deleted_at IS NULL;

-- Catalogo tecnico fixo de permissoes do sistema (nao e por tenant, nao tem soft delete).
CREATE TABLE permissions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(80) NOT NULL,
    module       VARCHAR(60) NOT NULL,
    action       VARCHAR(20) NOT NULL
                    CHECK (action IN ('VIEW', 'CREATE', 'EDIT', 'DELETE', 'EXPORT', 'IMPORT')),
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_permissions_code UNIQUE (code)
);

CREATE INDEX idx_permissions_module ON permissions (module);

CREATE TABLE role_permissions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id        UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id  UUID NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);

CREATE TABLE user_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_roles UNIQUE (user_id, role_id)
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
