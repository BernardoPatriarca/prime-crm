-- Fase 4 - Permissoes RBAC do modulo Propostas, concedidas integralmente ao perfil Administrador.
-- Os INSERTs sao idempotentes (NOT EXISTS) para tolerar reexecucao em bases parciais.

INSERT INTO permissions (code, module, action, description)
SELECT v.code, v.module, v.action, v.description
FROM (VALUES
    ('PROPOSTAS_VIEW',   'PROPOSTAS', 'VIEW',   'Visualizar propostas'),
    ('PROPOSTAS_CREATE', 'PROPOSTAS', 'CREATE', 'Criar propostas'),
    ('PROPOSTAS_EDIT',   'PROPOSTAS', 'EDIT',   'Editar propostas e seus itens'),
    ('PROPOSTAS_DELETE', 'PROPOSTAS', 'DELETE', 'Excluir propostas')
) AS v(code, module, action, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = v.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'Administrador'
  AND p.module = 'PROPOSTAS'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
