-- Seed inicial: catalogo de permissoes, role Administrador, usuario admin,
-- catalogo de domain_types e alguns domain_values de exemplo, e system_settings padrao.

-- =========================================================================
-- 1) Permissoes (catalogo tecnico fixo)
-- =========================================================================
INSERT INTO permissions (code, module, action, description) VALUES
    ('USUARIOS_VIEW',   'USUARIOS', 'VIEW',   'Visualizar usuarios'),
    ('USUARIOS_CREATE', 'USUARIOS', 'CREATE', 'Criar usuarios'),
    ('USUARIOS_EDIT',   'USUARIOS', 'EDIT',   'Editar usuarios'),
    ('USUARIOS_DELETE', 'USUARIOS', 'DELETE', 'Excluir usuarios'),

    ('PERFIS_VIEW',     'PERFIS', 'VIEW',   'Visualizar perfis de acesso'),
    ('PERFIS_CREATE',   'PERFIS', 'CREATE', 'Criar perfis de acesso'),
    ('PERFIS_EDIT',     'PERFIS', 'EDIT',   'Editar perfis de acesso'),
    ('PERFIS_DELETE',   'PERFIS', 'DELETE', 'Excluir perfis de acesso'),

    ('PERMISSOES_VIEW', 'PERMISSOES', 'VIEW', 'Visualizar catalogo de permissoes'),

    ('DOMINIOS_VIEW',   'DOMINIOS', 'VIEW',   'Visualizar cadastros genericos (dominios)'),
    ('DOMINIOS_CREATE', 'DOMINIOS', 'CREATE', 'Criar valores de dominio'),
    ('DOMINIOS_EDIT',   'DOMINIOS', 'EDIT',   'Editar valores de dominio'),
    ('DOMINIOS_DELETE', 'DOMINIOS', 'DELETE', 'Excluir valores de dominio'),

    ('PIPELINES_VIEW',   'PIPELINES', 'VIEW',   'Visualizar funis e etapas'),
    ('PIPELINES_CREATE', 'PIPELINES', 'CREATE', 'Criar funis e etapas'),
    ('PIPELINES_EDIT',   'PIPELINES', 'EDIT',   'Editar funis e etapas'),
    ('PIPELINES_DELETE', 'PIPELINES', 'DELETE', 'Excluir funis e etapas'),

    ('CAMPOS_PERSONALIZADOS_VIEW',   'CAMPOS_PERSONALIZADOS', 'VIEW',   'Visualizar campos personalizados'),
    ('CAMPOS_PERSONALIZADOS_CREATE', 'CAMPOS_PERSONALIZADOS', 'CREATE', 'Criar campos personalizados'),
    ('CAMPOS_PERSONALIZADOS_EDIT',   'CAMPOS_PERSONALIZADOS', 'EDIT',   'Editar campos personalizados'),
    ('CAMPOS_PERSONALIZADOS_DELETE', 'CAMPOS_PERSONALIZADOS', 'DELETE', 'Excluir campos personalizados'),

    ('TEMPLATES_VIEW',   'TEMPLATES', 'VIEW',   'Visualizar templates'),
    ('TEMPLATES_CREATE', 'TEMPLATES', 'CREATE', 'Criar templates'),
    ('TEMPLATES_EDIT',   'TEMPLATES', 'EDIT',   'Editar templates'),
    ('TEMPLATES_DELETE', 'TEMPLATES', 'DELETE', 'Excluir templates'),

    ('CONFIGURACOES_GERAIS_VIEW', 'CONFIGURACOES_GERAIS', 'VIEW', 'Visualizar configuracoes gerais'),
    ('CONFIGURACOES_GERAIS_EDIT', 'CONFIGURACOES_GERAIS', 'EDIT', 'Editar configuracoes gerais'),

    ('FERIADOS_VIEW',   'FERIADOS', 'VIEW',   'Visualizar feriados'),
    ('FERIADOS_CREATE', 'FERIADOS', 'CREATE', 'Criar feriados'),
    ('FERIADOS_EDIT',   'FERIADOS', 'EDIT',   'Editar feriados'),
    ('FERIADOS_DELETE', 'FERIADOS', 'DELETE', 'Excluir feriados');

-- =========================================================================
-- 2) Role Administrador com todas as permissoes
-- =========================================================================
INSERT INTO roles (name, description, active)
VALUES ('Administrador', 'Perfil com acesso completo a todos os modulos do sistema', true);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'Administrador';

-- =========================================================================
-- 3) Usuario seed admin@primecrm.local / login admin / senha Admin@123
--    Hash BCrypt (custo 10) gerado com org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.
-- =========================================================================
INSERT INTO users (name, email, login, password_hash, status)
VALUES (
    'Administrador',
    'admin@primecrm.local',
    'admin',
    '$2a$10$K6CAn2vyER/AeSbuQ1V9IeoceTRgis7HqUNad5KCi7FOkf4tNrjnq',
    'ACTIVE'
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.login = 'admin' AND r.name = 'Administrador';

-- =========================================================================
-- 4) Catalogo de domain_types (~15 cadastros genericos da secao 6 do spec)
-- =========================================================================
INSERT INTO domain_types (code, label, supports_color, supports_icon, system_defined) VALUES
    ('CLIENT_TYPE',      'Tipo de Cliente',        true,  true,  true),
    ('PERSON_TYPE',      'Tipo de Pessoa',         false, false, true),
    ('COMPANY_TYPE',     'Tipo de Empresa',        false, false, true),
    ('MARKET_SEGMENT',   'Segmento de Mercado',    true,  true,  true),
    ('ACTIVITY_BRANCH',  'Ramo de Atividade',      false, true,  true),
    ('LEAD_ORIGIN',      'Origem do Lead',         true,  true,  true),
    ('LOSS_REASON',      'Motivo de Perda',        true,  true,  true),
    ('WIN_REASON',       'Motivo de Ganho',        true,  true,  true),
    ('GENERIC_STATUS',   'Status Generico',        true,  false, true),
    ('PRIORITY',         'Prioridade',             true,  true,  true),
    ('TASK_TYPE',        'Tipo de Tarefa',         true,  true,  true),
    ('CATEGORY',         'Categoria',              true,  true,  true),
    ('TAG',              'Tag',                    true,  false, true),
    ('TEAM',             'Equipe',                 false, true,  true),
    ('POSITION',         'Cargo',                  false, false, true),
    ('DEPARTMENT',       'Departamento',           false, true,  true);

-- =========================================================================
-- 5) domain_values de exemplo
-- =========================================================================

-- CLIENT_TYPE
INSERT INTO domain_values (domain_type_id, code, name, display_order)
SELECT dt.id, v.code, v.name, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('PF', 'Pessoa Fisica', 1),
    ('PJ', 'Pessoa Juridica', 2),
    ('LEAD', 'Lead', 3),
    ('FORNECEDOR', 'Fornecedor', 4),
    ('PARCEIRO', 'Parceiro', 5)
) AS v(code, name, display_order)
WHERE dt.code = 'CLIENT_TYPE';

-- LEAD_ORIGIN
INSERT INTO domain_values (domain_type_id, code, name, display_order)
SELECT dt.id, v.code, v.name, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('SITE', 'Site', 1),
    ('INDICACAO', 'Indicacao', 2),
    ('REDES_SOCIAIS', 'Redes Sociais', 3),
    ('EVENTO', 'Evento', 4),
    ('COLD_CALL', 'Cold Call', 5)
) AS v(code, name, display_order)
WHERE dt.code = 'LEAD_ORIGIN';

-- PRIORITY (com cor)
INSERT INTO domain_values (domain_type_id, code, name, color, display_order)
SELECT dt.id, v.code, v.name, v.color, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('BAIXA', 'Baixa', '#22C55E', 1),
    ('MEDIA', 'Media', '#EAB308', 2),
    ('ALTA', 'Alta', '#F97316', 3),
    ('URGENTE', 'Urgente', '#EF4444', 4)
) AS v(code, name, color, display_order)
WHERE dt.code = 'PRIORITY';

-- TASK_TYPE
INSERT INTO domain_values (domain_type_id, code, name, icon, display_order)
SELECT dt.id, v.code, v.name, v.icon, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('LIGACAO', 'Ligacao', 'pi-phone', 1),
    ('REUNIAO', 'Reuniao', 'pi-users', 2),
    ('EMAIL', 'E-mail', 'pi-envelope', 3),
    ('VISITA', 'Visita', 'pi-map-marker', 4),
    ('FOLLOW_UP', 'Follow-up', 'pi-refresh', 5)
) AS v(code, name, icon, display_order)
WHERE dt.code = 'TASK_TYPE';

-- TAG (exemplos com cor)
INSERT INTO domain_values (domain_type_id, code, name, color, display_order)
SELECT dt.id, v.code, v.name, v.color, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('VIP', 'VIP', '#A855F7', 1),
    ('INADIMPLENTE', 'Inadimplente', '#EF4444', 2),
    ('NOVO', 'Novo', '#22C55E', 3),
    ('RECORRENTE', 'Recorrente', '#3B82F6', 4)
) AS v(code, name, color, display_order)
WHERE dt.code = 'TAG';

-- =========================================================================
-- 6) system_settings padrao
-- =========================================================================
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
    ('default_currency', 'BRL', 'Moeda padrao utilizada em valores monetarios'),
    ('default_timezone', 'America/Sao_Paulo', 'Fuso horario padrao do sistema'),
    ('date_format', 'dd/MM/yyyy', 'Formato padrao de exibicao de datas'),
    ('business_days', 'SEG-SEX', 'Dias uteis padrao (string simples; SEG-SEX = segunda a sexta)');
