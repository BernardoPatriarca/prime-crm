-- Fase 4 - Permissoes RBAC do modulo Pedidos, concedidas integralmente ao perfil Administrador.
-- Os INSERTs sao idempotentes (NOT EXISTS) para tolerar reexecucao em bases parciais.

INSERT INTO permissions (code, module, action, description)
SELECT v.code, v.module, v.action, v.description
FROM (VALUES
    ('PEDIDOS_VIEW',   'PEDIDOS', 'VIEW',   'Visualizar pedidos'),
    ('PEDIDOS_CREATE', 'PEDIDOS', 'CREATE', 'Criar pedidos'),
    ('PEDIDOS_EDIT',   'PEDIDOS', 'EDIT',   'Editar pedidos e seus itens'),
    ('PEDIDOS_DELETE', 'PEDIDOS', 'DELETE', 'Excluir pedidos')
) AS v(code, module, action, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = v.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'Administrador'
  AND p.module = 'PEDIDOS'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
