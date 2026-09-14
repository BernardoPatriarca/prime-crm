-- Fase 4 - Itens da proposta. Preco e um snapshot no momento em que o item e adicionado
-- (nao acompanha alteracoes futuras no preco do produto, senao propostas ja enviadas mudariam
-- de valor sozinhas).
CREATE TABLE proposal_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    proposal_id       UUID NOT NULL REFERENCES proposals (id),
    product_id        UUID NOT NULL REFERENCES products (id),
    description       VARCHAR(300),
    quantity          NUMERIC(15, 3) NOT NULL,
    unit_price        NUMERIC(15, 2) NOT NULL,
    discount_percent  NUMERIC(5, 2) NOT NULL DEFAULT 0
                          CHECK (discount_percent >= 0 AND discount_percent <= 100),
    display_order     INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(120),
    updated_by        VARCHAR(120),
    deleted_at        TIMESTAMPTZ,
    CHECK (quantity > 0)
);

CREATE INDEX idx_proposal_items_tenant_id ON proposal_items (tenant_id);
CREATE INDEX idx_proposal_items_proposal_id ON proposal_items (proposal_id);
CREATE INDEX idx_proposal_items_product_id ON proposal_items (product_id);
CREATE INDEX idx_proposal_items_not_deleted ON proposal_items (id) WHERE deleted_at IS NULL;
