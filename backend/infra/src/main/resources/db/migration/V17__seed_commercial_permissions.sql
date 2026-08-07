-- Fase 2 - Nucleo Comercial: permissoes RBAC dos modulos Clientes, Contatos,
-- Leads e Oportunidades, concedidas integralmente ao perfil Administrador.
-- Os INSERTs sao idempotentes (NOT EXISTS) para tolerar reexecucao em bases parciais.

INSERT INTO permissions (code, module, action, description)
SELECT v.code, v.module, v.action, v.description
FROM (VALUES
    ('CLIENTES_VIEW',   'CLIENTES', 'VIEW',   'Visualizar clientes e empresas'),
    ('CLIENTES_CREATE', 'CLIENTES', 'CREATE', 'Criar clientes e empresas'),
    ('CLIENTES_EDIT',   'CLIENTES', 'EDIT',   'Editar clientes e empresas'),
    ('CLIENTES_DELETE', 'CLIENTES', 'DELETE', 'Excluir clientes e empresas'),
    ('CLIENTES_EXPORT', 'CLIENTES', 'EXPORT', 'Exportar clientes e empresas'),

    ('CONTATOS_VIEW',   'CONTATOS', 'VIEW',   'Visualizar contatos'),
    ('CONTATOS_CREATE', 'CONTATOS', 'CREATE', 'Criar contatos'),
    ('CONTATOS_EDIT',   'CONTATOS', 'EDIT',   'Editar contatos'),
    ('CONTATOS_DELETE', 'CONTATOS', 'DELETE', 'Excluir contatos'),

    ('LEADS_VIEW',   'LEADS', 'VIEW',   'Visualizar leads'),
    ('LEADS_CREATE', 'LEADS', 'CREATE', 'Criar leads'),
    ('LEADS_EDIT',   'LEADS', 'EDIT',   'Editar leads'),
    ('LEADS_DELETE', 'LEADS', 'DELETE', 'Excluir leads'),
    ('LEADS_EXPORT', 'LEADS', 'EXPORT', 'Exportar leads'),

    ('OPORTUNIDADES_VIEW',   'OPORTUNIDADES', 'VIEW',   'Visualizar oportunidades'),
    ('OPORTUNIDADES_CREATE', 'OPORTUNIDADES', 'CREATE', 'Criar oportunidades'),
    ('OPORTUNIDADES_EDIT',   'OPORTUNIDADES', 'EDIT',   'Editar oportunidades'),
    ('OPORTUNIDADES_DELETE', 'OPORTUNIDADES', 'DELETE', 'Excluir oportunidades')
) AS v(code, module, action, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = v.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'Administrador'
  AND p.module IN ('CLIENTES', 'CONTATOS', 'LEADS', 'OPORTUNIDADES')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
