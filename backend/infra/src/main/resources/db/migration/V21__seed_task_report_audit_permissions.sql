-- Fase 3 - Permissoes RBAC dos modulos Tarefas, Relatorios e Auditoria,
-- concedidas integralmente ao perfil Administrador.
-- Os INSERTs sao idempotentes (NOT EXISTS) para tolerar reexecucao em bases parciais.

INSERT INTO permissions (code, module, action, description)
SELECT v.code, v.module, v.action, v.description
FROM (VALUES
    ('TAREFAS_VIEW',   'TAREFAS', 'VIEW',   'Visualizar tarefas'),
    ('TAREFAS_CREATE', 'TAREFAS', 'CREATE', 'Criar tarefas'),
    ('TAREFAS_EDIT',   'TAREFAS', 'EDIT',   'Editar tarefas'),
    ('TAREFAS_DELETE', 'TAREFAS', 'DELETE', 'Excluir tarefas'),
    ('TAREFAS_EXPORT', 'TAREFAS', 'EXPORT', 'Exportar tarefas'),

    ('RELATORIOS_VIEW',   'RELATORIOS', 'VIEW',   'Visualizar relatorios'),
    ('RELATORIOS_EXPORT', 'RELATORIOS', 'EXPORT', 'Exportar relatorios em CSV'),

    ('AUDITORIA_VIEW',   'AUDITORIA', 'VIEW',   'Visualizar o log de auditoria'),
    ('AUDITORIA_EXPORT', 'AUDITORIA', 'EXPORT', 'Exportar o log de auditoria em CSV')
) AS v(code, module, action, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions p WHERE p.code = v.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'Administrador'
  AND p.module IN ('TAREFAS', 'RELATORIOS', 'AUDITORIA')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
