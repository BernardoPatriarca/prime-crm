-- Fase 4 - Produtos: catalogo de produtos e servicos, base para Propostas/Pedidos/Contratos
-- (ainda nao construidos). Categoria e unidade de medida reaproveitam o engine domain_values.
CREATE SEQUENCE product_code_seq START 1000;

CREATE TABLE products (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    code          VARCHAR(30) DEFAULT ('PRD-' || lpad(nextval('product_code_seq')::text, 6, '0')),
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    sku           VARCHAR(60),
    category_id   UUID REFERENCES domain_values (id),
    unit_id       UUID REFERENCES domain_values (id),
    unit_price    NUMERIC(15, 2) NOT NULL DEFAULT 0,
    cost_price    NUMERIC(15, 2),
    is_service    BOOLEAN NOT NULL DEFAULT false,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    VARCHAR(120),
    updated_by    VARCHAR(120),
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX idx_products_tenant_id ON products (tenant_id);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_unit_id ON products (unit_id);
CREATE INDEX idx_products_not_deleted ON products (id) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_tenant_name ON products (tenant_id, name) WHERE deleted_at IS NULL;
CREATE INDEX idx_products_tenant_active ON products (tenant_id, active) WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_products_tenant_code
    ON products (tenant_id, code)
    WHERE code IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uq_products_tenant_sku
    ON products (tenant_id, sku)
    WHERE sku IS NOT NULL AND deleted_at IS NULL;
