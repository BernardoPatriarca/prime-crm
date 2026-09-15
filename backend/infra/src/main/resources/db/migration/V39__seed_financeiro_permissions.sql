-- Fase 5 - Permissoes RBAC do modulo Financeiro (Contas a Receber), concedidas integralmente
-- ao perfil Administrador. Os INSERTs sao idempotentes (NOT EXISTS) para tolerar reexecucao
-- em bases parciais.

INSERT INTO permissions (code, module, action, description)
SELECT v.code, v.module, v.action, v.description
FROM (VALUES
    ('FINANCEIRO_VIEW',   'FINANCEIRO', 'VIEW',   'Visualizar contas a receber'),
    ('FINANCEIRO_CREATE', 'FINANCEIRO', 'CREATE', 'Criar contas a receber'),
    ('FINANCEIRO_EDIT',   'FINANCEIRO', 'EDIT',   'Editar e baixar contas a receber'),
    ('FINANCEIRO_DELETE', 'FINANCEIRO', 'DELETE', 'Excluir contas a receber')
) AS v(code, module, action, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = v.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'Administrador'
  AND p.module = 'FINANCEIRO'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
