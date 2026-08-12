-- =====================================================================
-- Prime CRM — massa de dados de demonstracao
--
-- Objetivo: deixar o sistema com cara de "ja em uso" para avaliacao.
-- Seguro rodar mais de uma vez: todo INSERT e protegido por NOT EXISTS.
-- Nao apaga nada. Para limpar, use o bloco de rollback no final do arquivo.
--
-- Uso:  psql -U postgres -h localhost -d primecrm -f scripts/demo-data.sql
--
-- Senha de todos os usuarios criados aqui: Admin@123
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Cadastros gerais (domain_values) — os 16 catalogos parametrizaveis
-- ---------------------------------------------------------------------
INSERT INTO domain_values (domain_type_id, code, name, description, color, icon, display_order, active)
SELECT dt.id, v.code, v.name, v.description, v.color, v.icon, v.display_order::int, v.active::boolean
FROM (VALUES
    -- Tipo de Cliente
    ('CLIENT_TYPE','PF','Pessoa Fisica','Cliente pessoa fisica','#3B82F6','pi pi-user',1,true),
    ('CLIENT_TYPE','PJ','Pessoa Juridica','Empresa com CNPJ','#0B3C91','pi pi-building',2,true),
    ('CLIENT_TYPE','LEAD','Lead','Contato ainda nao qualificado','#F59E0B','pi pi-filter',3,true),
    ('CLIENT_TYPE','FORNECEDOR','Fornecedor','Parceiro de suprimentos','#71809A','pi pi-truck',4,true),
    ('CLIENT_TYPE','PARCEIRO','Parceiro','Parceiro comercial / revenda','#22C55E','pi pi-share-alt',5,true),
    ('CLIENT_TYPE','EX_CLIENTE','Ex-cliente','Relacionamento encerrado','#EF4444','pi pi-user-minus',6,false),
    -- Tipo de Pessoa
    ('PERSON_TYPE','FISICA','Fisica','Pessoa fisica',NULL,NULL,1,true),
    ('PERSON_TYPE','JURIDICA','Juridica','Pessoa juridica',NULL,NULL,2,true),
    ('PERSON_TYPE','ESTRANGEIRO','Estrangeiro','Sem CPF/CNPJ nacional',NULL,NULL,3,true),
    -- Tipo de Empresa
    ('COMPANY_TYPE','MATRIZ','Matriz','Sede da empresa',NULL,NULL,1,true),
    ('COMPANY_TYPE','FILIAL','Filial','Unidade filial',NULL,NULL,2,true),
    ('COMPANY_TYPE','FRANQUIA','Franquia','Unidade franqueada',NULL,NULL,3,true),
    ('COMPANY_TYPE','CD','Centro de Distribuicao','Operacao logistica',NULL,NULL,4,true),
    -- Segmentos de Mercado
    ('MARKET_SEGMENT','VAREJO','Varejo','Comercio ao consumidor final','#3B82F6','pi pi-shopping-cart',1,true),
    ('MARKET_SEGMENT','ATACADO','Atacado','Venda em grande volume','#1E5EFF','pi pi-box',2,true),
    ('MARKET_SEGMENT','INDUSTRIA','Industria','Manufatura e producao','#0B3C91','pi pi-cog',3,true),
    ('MARKET_SEGMENT','SERVICOS','Servicos','Prestacao de servicos','#22C55E','pi pi-briefcase',4,true),
    ('MARKET_SEGMENT','TECNOLOGIA','Tecnologia','Software e TI','#0EA5E9','pi pi-desktop',5,true),
    ('MARKET_SEGMENT','SAUDE','Saude','Clinicas, hospitais e laboratorios','#EF4444','pi pi-heart',6,true),
    ('MARKET_SEGMENT','EDUCACAO','Educacao','Escolas e cursos','#F59E0B','pi pi-book',7,true),
    ('MARKET_SEGMENT','AGRO','Agronegocio','Producao agropecuaria','#22C55E','pi pi-sun',8,true),
    ('MARKET_SEGMENT','CONSTRUCAO','Construcao Civil','Obras e incorporacao','#71809A','pi pi-home',9,true),
    ('MARKET_SEGMENT','FINANCEIRO','Financeiro','Bancos e seguradoras','#0B3C91','pi pi-dollar',10,true),
    ('MARKET_SEGMENT','LOGISTICA','Logistica','Transporte e armazenagem','#576078','pi pi-send',11,true),
    ('MARKET_SEGMENT','IMOBILIARIO','Imobiliario','Imobiliarias e corretoras','#3B82F6','pi pi-map',12,true),
    -- Ramos de Atividade
    ('ACTIVITY_BRANCH','SOFTWARE','Desenvolvimento de Software','CNAE 6201-5',NULL,'pi pi-code',1,true),
    ('ACTIVITY_BRANCH','CONSULTORIA','Consultoria Empresarial','CNAE 7020-4',NULL,'pi pi-users',2,true),
    ('ACTIVITY_BRANCH','COMERCIO_VAREJISTA','Comercio Varejista','CNAE 4711-3',NULL,'pi pi-shopping-bag',3,true),
    ('ACTIVITY_BRANCH','COMERCIO_ATACADISTA','Comercio Atacadista','CNAE 4691-5',NULL,'pi pi-box',4,true),
    ('ACTIVITY_BRANCH','INDUSTRIA_ALIMENTOS','Industria de Alimentos','CNAE 1091-1',NULL,'pi pi-shopping-cart',5,true),
    ('ACTIVITY_BRANCH','METALURGIA','Metalurgia','CNAE 2451-2',NULL,'pi pi-wrench',6,true),
    ('ACTIVITY_BRANCH','TRANSPORTE','Transporte Rodoviario','CNAE 4930-2',NULL,'pi pi-truck',7,true),
    ('ACTIVITY_BRANCH','MARKETING','Agencia de Marketing','CNAE 7311-4',NULL,'pi pi-megaphone',8,true),
    ('ACTIVITY_BRANCH','CONTABILIDADE','Servicos Contabeis','CNAE 6920-6',NULL,'pi pi-calculator',9,true),
    ('ACTIVITY_BRANCH','ENGENHARIA','Servicos de Engenharia','CNAE 7112-0',NULL,'pi pi-compass',10,true),
    -- Origens do Lead
    ('LEAD_ORIGIN','SITE','Site','Formulario do site institucional','#3B82F6','pi pi-globe',1,true),
    ('LEAD_ORIGIN','INDICACAO','Indicacao','Indicado por cliente ou parceiro','#22C55E','pi pi-users',2,true),
    ('LEAD_ORIGIN','REDES_SOCIAIS','Redes Sociais','Instagram, LinkedIn, Facebook','#0EA5E9','pi pi-thumbs-up',3,true),
    ('LEAD_ORIGIN','EVENTO','Evento','Feira, congresso ou workshop','#F59E0B','pi pi-calendar',4,true),
    ('LEAD_ORIGIN','COLD_CALL','Cold Call','Prospeccao ativa por telefone','#71809A','pi pi-phone',5,true),
    ('LEAD_ORIGIN','INBOUND','Inbound','Conteudo, blog e materiais ricos','#1E5EFF','pi pi-download',6,true),
    ('LEAD_ORIGIN','OUTBOUND','Outbound','Prospeccao ativa por e-mail','#576078','pi pi-send',7,true),
    ('LEAD_ORIGIN','GOOGLE_ADS','Google Ads','Campanha de midia paga','#EF4444','pi pi-search',8,true),
    ('LEAD_ORIGIN','META_ADS','Meta Ads','Campanha Facebook/Instagram','#0B3C91','pi pi-facebook',9,true),
    ('LEAD_ORIGIN','MARKETPLACE','Marketplace','Origem em marketplace parceiro','#F59E0B','pi pi-shopping-cart',10,false),
    -- Motivos de Perda
    ('LOSS_REASON','PRECO','Preco acima do orcamento','Cliente considerou o valor alto','#EF4444','pi pi-dollar',1,true),
    ('LOSS_REASON','CONCORRENTE','Escolheu concorrente','Fechou com outro fornecedor','#EF4444','pi pi-flag',2,true),
    ('LOSS_REASON','SEM_BUDGET','Sem verba no momento','Adiado por restricao orcamentaria','#F59E0B','pi pi-wallet',3,true),
    ('LOSS_REASON','SEM_RESPOSTA','Sem resposta','Contato parou de responder','#71809A','pi pi-volume-off',4,true),
    ('LOSS_REASON','FORA_PERFIL','Fora do perfil','Nao atende ao ICP','#576078','pi pi-ban',5,true),
    ('LOSS_REASON','TIMING','Timing inadequado','Projeto adiado pelo cliente','#F59E0B','pi pi-clock',6,true),
    ('LOSS_REASON','ESCOPO','Escopo incompativel','Necessidade fora do produto','#71809A','pi pi-times-circle',7,true),
    -- Motivos de Ganho
    ('WIN_REASON','MELHOR_PRECO','Melhor preco','Proposta mais competitiva','#22C55E','pi pi-dollar',1,true),
    ('WIN_REASON','RELACIONAMENTO','Relacionamento','Confianca construida com o cliente','#22C55E','pi pi-heart',2,true),
    ('WIN_REASON','PRODUTO','Aderencia do produto','Solucao atendeu melhor a necessidade','#1E5EFF','pi pi-check-circle',3,true),
    ('WIN_REASON','PRAZO','Prazo de entrega','Entrega mais rapida que a concorrencia','#0EA5E9','pi pi-clock',4,true),
    ('WIN_REASON','SUPORTE','Suporte e pos-venda','Diferencial de atendimento','#22C55E','pi pi-headphones',5,true),
    ('WIN_REASON','INDICACAO_GANHO','Indicacao forte','Chegou recomendado','#3B82F6','pi pi-users',6,true),
    -- Status generico
    ('GENERIC_STATUS','NOVO','Novo','Registro recem-criado','#0EA5E9','pi pi-star',1,true),
    ('GENERIC_STATUS','EM_ANDAMENTO','Em andamento','Em tratativa','#3B82F6','pi pi-spinner',2,true),
    ('GENERIC_STATUS','AGUARDANDO','Aguardando retorno','Pendente de terceiros','#F59E0B','pi pi-hourglass',3,true),
    ('GENERIC_STATUS','CONCLUIDO','Concluido','Finalizado com sucesso','#22C55E','pi pi-check',4,true),
    ('GENERIC_STATUS','CANCELADO','Cancelado','Encerrado sem conclusao','#EF4444','pi pi-times',5,true),
    ('GENERIC_STATUS','SUSPENSO','Suspenso','Temporariamente parado','#71809A','pi pi-pause',6,true),
    -- Prioridades
    ('PRIORITY','BAIXA','Baixa','Sem urgencia','#22C55E','pi pi-angle-down',1,true),
    ('PRIORITY','MEDIA','Media','Prazo normal','#F59E0B','pi pi-minus',2,true),
    ('PRIORITY','ALTA','Alta','Requer atencao','#EF4444','pi pi-angle-up',3,true),
    ('PRIORITY','URGENTE','Urgente','Tratar imediatamente','#0B3C91','pi pi-angle-double-up',4,true),
    -- Tipos de Tarefa
    ('TASK_TYPE','LIGACAO','Ligacao','Contato telefonico','#3B82F6','pi pi-phone',1,true),
    ('TASK_TYPE','REUNIAO','Reuniao','Reuniao presencial ou online','#0B3C91','pi pi-users',2,true),
    ('TASK_TYPE','EMAIL','E-mail','Envio de e-mail','#0EA5E9','pi pi-envelope',3,true),
    ('TASK_TYPE','VISITA','Visita','Visita ao cliente','#22C55E','pi pi-map-marker',4,true),
    ('TASK_TYPE','FOLLOW_UP','Follow-up','Retomada de contato','#F59E0B','pi pi-replay',5,true),
    ('TASK_TYPE','APRESENTACAO','Apresentacao','Demonstracao da solucao','#1E5EFF','pi pi-desktop',6,true),
    ('TASK_TYPE','PROPOSTA','Envio de Proposta','Elaboracao e envio de proposta','#576078','pi pi-file',7,true),
    ('TASK_TYPE','WHATSAPP','WhatsApp','Contato por mensagem','#22C55E','pi pi-comment',8,true),
    -- Categorias
    ('CATEGORY','ESTRATEGICO','Estrategico','Conta estrategica','#0B3C91','pi pi-star-fill',1,true),
    ('CATEGORY','RECORRENTE','Recorrente','Receita recorrente','#22C55E','pi pi-sync',2,true),
    ('CATEGORY','PONTUAL','Pontual','Venda avulsa','#71809A','pi pi-circle',3,true),
    ('CATEGORY','UPSELL','Upsell','Expansao de conta','#1E5EFF','pi pi-arrow-up-right',4,true),
    ('CATEGORY','RENOVACAO','Renovacao','Renovacao de contrato','#0EA5E9','pi pi-refresh',5,true),
    -- Tags
    ('TAG','VIP','VIP','Cliente prioritario','#F59E0B','pi pi-star',1,true),
    ('TAG','INADIMPLENTE','Inadimplente','Pendencia financeira','#EF4444','pi pi-exclamation-triangle',2,true),
    ('TAG','POTENCIAL','Alto potencial','Boa oportunidade de expansao','#22C55E','pi pi-chart-line',3,true),
    ('TAG','FRIO','Contato frio','Baixo engajamento','#71809A','pi pi-snowflake',4,true),
    ('TAG','QUENTE','Contato quente','Alto engajamento','#EF4444','pi pi-bolt',5,true),
    ('TAG','CHURN_RISCO','Risco de churn','Sinais de cancelamento','#F59E0B','pi pi-exclamation-circle',6,true),
    ('TAG','EMBAIXADOR','Embaixador','Promotor da marca','#0B3C91','pi pi-megaphone',7,true),
    -- Equipes
    ('TEAM','COMERCIAL_SP','Comercial SP','Time comercial Sao Paulo','#1E5EFF','pi pi-users',1,true),
    ('TEAM','COMERCIAL_RJ','Comercial RJ','Time comercial Rio de Janeiro','#3B82F6','pi pi-users',2,true),
    ('TEAM','COMERCIAL_SUL','Comercial Sul','Time comercial regiao Sul','#0EA5E9','pi pi-users',3,true),
    ('TEAM','INSIDE_SALES','Inside Sales','Vendas internas','#0B3C91','pi pi-headphones',4,true),
    ('TEAM','CS','Customer Success','Pos-venda e retencao','#22C55E','pi pi-heart',5,true),
    ('TEAM','MARKETING_TIME','Marketing','Geracao de demanda','#F59E0B','pi pi-megaphone',6,true),
    ('TEAM','FINANCEIRO_TIME','Financeiro','Contas a pagar e receber','#576078','pi pi-dollar',7,true),
    -- Cargos
    ('POSITION','DIRETOR','Diretor Comercial','Direcao da area comercial',NULL,'pi pi-crown',1,true),
    ('POSITION','GERENTE','Gerente de Vendas','Gestao de equipe comercial',NULL,'pi pi-briefcase',2,true),
    ('POSITION','SUPERVISOR','Supervisor','Supervisao operacional',NULL,'pi pi-eye',3,true),
    ('POSITION','EXECUTIVO','Executivo de Contas','Vendas e relacionamento',NULL,'pi pi-user',4,true),
    ('POSITION','SDR','SDR','Pre-vendas e qualificacao',NULL,'pi pi-filter',5,true),
    ('POSITION','ANALISTA_CS','Analista de CS','Sucesso do cliente',NULL,'pi pi-heart',6,true),
    ('POSITION','ANALISTA_MKT','Analista de Marketing','Campanhas e conteudo',NULL,'pi pi-megaphone',7,true),
    ('POSITION','ANALISTA_FIN','Analista Financeiro','Rotinas financeiras',NULL,'pi pi-calculator',8,true),
    ('POSITION','ESTAGIARIO','Estagiario','Apoio operacional',NULL,'pi pi-user-plus',9,true),
    -- Departamentos
    ('DEPARTMENT','COMERCIAL','Comercial','Area de vendas','#1E5EFF','pi pi-chart-line',1,true),
    ('DEPARTMENT','MARKETING_DEP','Marketing','Geracao de demanda','#F59E0B','pi pi-megaphone',2,true),
    ('DEPARTMENT','FINANCEIRO_DEP','Financeiro','Gestao financeira','#22C55E','pi pi-dollar',3,true),
    ('DEPARTMENT','OPERACOES','Operacoes','Entrega e operacao','#576078','pi pi-cog',4,true),
    ('DEPARTMENT','TI','Tecnologia','Sistemas e infraestrutura','#0B3C91','pi pi-desktop',5,true),
    ('DEPARTMENT','RH','Recursos Humanos','Pessoas e cultura','#0EA5E9','pi pi-users',6,true),
    ('DEPARTMENT','DIRETORIA','Diretoria','Alta gestao','#0B3C91','pi pi-crown',7,true)
) AS v(type_code, code, name, description, color, icon, display_order, active)
JOIN domain_types dt ON dt.code = v.type_code
WHERE NOT EXISTS (
    SELECT 1 FROM domain_values dv WHERE dv.domain_type_id = dt.id AND dv.code = v.code
);

-- ---------------------------------------------------------------------
-- 2. Perfis de acesso (roles)
-- ---------------------------------------------------------------------
INSERT INTO roles (name, description, active)
SELECT v.name, v.description, v.active::boolean
FROM (VALUES
    ('Gerente','Gestao completa da operacao comercial',true),
    ('Supervisor','Acompanha e edita registros da equipe',true),
    ('Comercial','Executivo de vendas: cria e edita seus registros',true),
    ('Financeiro','Acesso a dados financeiros e configuracoes gerais',true),
    ('Atendimento','Suporte e pos-venda, acesso majoritariamente de leitura',true),
    ('Marketing','Campanhas, origens de lead e templates',true),
    ('Usuario Padrao','Somente leitura nos modulos liberados',true),
    ('Auditoria','Perfil de leitura para conferencia (desativado)',false)
) AS v(name, description, active)
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.name = v.name);

-- Permissoes por perfil (por modulo e acao)
-- Os modulos comerciais (CLIENTES, CONTATOS, LEADS, OPORTUNIDADES) so existem a partir
-- da migration V17. Como o INSERT abaixo e protegido por NOT EXISTS por par
-- (perfil, permissao), rodar este script de novo depois de novas permissoes surgirem
-- concede o que estiver faltando, sem duplicar nada.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON (
    (r.name = 'Gerente')
 OR (r.name = 'Supervisor'    AND p.action IN ('VIEW','CREATE','EDIT','EXPORT'))
 OR (r.name = 'Comercial'     AND (
        p.action = 'VIEW'
     OR (p.module IN ('CLIENTES','CONTATOS','LEADS','OPORTUNIDADES') AND p.action IN ('CREATE','EDIT','EXPORT'))
     OR (p.module IN ('PIPELINES','DOMINIOS') AND p.action IN ('CREATE','EDIT'))))
 OR (r.name = 'Financeiro'    AND (p.action = 'VIEW' OR p.module = 'CONFIGURACOES_GERAIS'))
 OR (r.name = 'Atendimento'   AND (
        p.action = 'VIEW'
     OR (p.module IN ('CLIENTES','CONTATOS') AND p.action = 'EDIT')))
 OR (r.name = 'Marketing'     AND (
        p.action = 'VIEW'
     OR (p.module IN ('TEMPLATES','DOMINIOS') AND p.action IN ('CREATE','EDIT','DELETE'))
     OR (p.module = 'LEADS' AND p.action IN ('CREATE','EDIT','EXPORT'))))
 OR (r.name = 'Usuario Padrao' AND p.action = 'VIEW'
        AND p.module IN ('DOMINIOS','PIPELINES','FERIADOS','CLIENTES','LEADS','OPORTUNIDADES'))
 OR (r.name = 'Auditoria'     AND p.action = 'VIEW')
)
WHERE r.name IN ('Gerente','Supervisor','Comercial','Financeiro','Atendimento','Marketing','Usuario Padrao','Auditoria')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- 3. Usuarios (senha de todos: Admin@123)
-- ---------------------------------------------------------------------
INSERT INTO users (name, email, login, password_hash, phone, status, theme_preference,
                   department_domain_value_id, team_domain_value_id, position_domain_value_id,
                   last_login_at, created_at)
SELECT
    v.name, v.email, v.login,
    '$2a$10$K6CAn2vyER/AeSbuQ1V9IeoceTRgis7HqUNad5KCi7FOkf4tNrjnq',
    v.phone, v.status, v.theme,
    (SELECT dv.id FROM domain_values dv JOIN domain_types dt ON dt.id = dv.domain_type_id
      WHERE dt.code = 'DEPARTMENT' AND dv.code = v.dep),
    (SELECT dv.id FROM domain_values dv JOIN domain_types dt ON dt.id = dv.domain_type_id
      WHERE dt.code = 'TEAM' AND dv.code = v.team),
    (SELECT dv.id FROM domain_values dv JOIN domain_types dt ON dt.id = dv.domain_type_id
      WHERE dt.code = 'POSITION' AND dv.code = v.pos),
    now() - (v.dias_ultimo_acesso::int * interval '1 day'),
    now() - (v.dias_cadastro::int * interval '1 day')
FROM (VALUES
    ('Carla Menezes','carla.menezes@primecrm.local','carla.menezes','(11) 98812-4477','ACTIVE','LIGHT','DIRETORIA','COMERCIAL_SP','DIRETOR',0,420),
    ('Rafael Duarte','rafael.duarte@primecrm.local','rafael.duarte','(11) 99654-2210','ACTIVE','DARK','COMERCIAL','COMERCIAL_SP','GERENTE',0,390),
    ('Juliana Prado','juliana.prado@primecrm.local','juliana.prado','(21) 98771-6633','ACTIVE','LIGHT','COMERCIAL','COMERCIAL_RJ','GERENTE',1,365),
    ('Marcos Vinicius Lima','marcos.lima@primecrm.local','marcos.lima','(51) 99123-8890','ACTIVE','DARK','COMERCIAL','COMERCIAL_SUL','SUPERVISOR',1,340),
    ('Patricia Nogueira','patricia.nogueira@primecrm.local','patricia.nogueira','(11) 98123-4590','ACTIVE','LIGHT','COMERCIAL','COMERCIAL_SP','EXECUTIVO',0,320),
    ('Diego Fontes','diego.fontes@primecrm.local','diego.fontes','(11) 99876-1122','ACTIVE','LIGHT','COMERCIAL','COMERCIAL_SP','EXECUTIVO',2,310),
    ('Amanda Ribeiro','amanda.ribeiro@primecrm.local','amanda.ribeiro','(21) 98456-7788','ACTIVE','DARK','COMERCIAL','COMERCIAL_RJ','EXECUTIVO',0,295),
    ('Bruno Tavares','bruno.tavares@primecrm.local','bruno.tavares','(21) 99332-4455','ACTIVE','LIGHT','COMERCIAL','COMERCIAL_RJ','EXECUTIVO',3,280),
    ('Leticia Barbosa','leticia.barbosa@primecrm.local','leticia.barbosa','(51) 98221-3344','ACTIVE','LIGHT','COMERCIAL','COMERCIAL_SUL','EXECUTIVO',1,270),
    ('Thiago Moreira','thiago.moreira@primecrm.local','thiago.moreira','(51) 99887-5566','ACTIVE','DARK','COMERCIAL','COMERCIAL_SUL','EXECUTIVO',5,265),
    ('Fernanda Castro','fernanda.castro@primecrm.local','fernanda.castro','(11) 98090-7712','ACTIVE','LIGHT','COMERCIAL','INSIDE_SALES','SDR',0,240),
    ('Gustavo Pereira','gustavo.pereira@primecrm.local','gustavo.pereira','(11) 99001-8823','ACTIVE','LIGHT','COMERCIAL','INSIDE_SALES','SDR',1,235),
    ('Larissa Andrade','larissa.andrade@primecrm.local','larissa.andrade','(11) 98332-9911','ACTIVE','DARK','COMERCIAL','INSIDE_SALES','SDR',2,220),
    ('Rodrigo Salles','rodrigo.salles@primecrm.local','rodrigo.salles','(11) 99445-2277','ACTIVE','LIGHT','COMERCIAL','INSIDE_SALES','SDR',8,210),
    ('Camila Rocha','camila.rocha@primecrm.local','camila.rocha','(11) 98664-3300','ACTIVE','LIGHT','OPERACOES','CS','ANALISTA_CS',0,200),
    ('Vinicius Alencar','vinicius.alencar@primecrm.local','vinicius.alencar','(11) 99553-8877','ACTIVE','DARK','OPERACOES','CS','ANALISTA_CS',1,190),
    ('Beatriz Campos','beatriz.campos@primecrm.local','beatriz.campos','(11) 98776-1234','ACTIVE','LIGHT','OPERACOES','CS','ANALISTA_CS',4,180),
    ('Henrique Souza','henrique.souza@primecrm.local','henrique.souza','(11) 99887-4321','ACTIVE','LIGHT','MARKETING_DEP','MARKETING_TIME','ANALISTA_MKT',0,175),
    ('Isabela Martins','isabela.martins@primecrm.local','isabela.martins','(11) 98443-9988','ACTIVE','DARK','MARKETING_DEP','MARKETING_TIME','ANALISTA_MKT',2,160),
    ('Eduardo Ramos','eduardo.ramos@primecrm.local','eduardo.ramos','(11) 99220-6655','ACTIVE','LIGHT','FINANCEIRO_DEP','FINANCEIRO_TIME','ANALISTA_FIN',1,150),
    ('Simone Vasques','simone.vasques@primecrm.local','simone.vasques','(11) 98110-7744','ACTIVE','LIGHT','FINANCEIRO_DEP','FINANCEIRO_TIME','ANALISTA_FIN',3,140),
    ('Paulo Cesar Nunes','paulo.nunes@primecrm.local','paulo.nunes','(11) 99334-1199','INACTIVE','LIGHT','COMERCIAL','COMERCIAL_SP','EXECUTIVO',95,320),
    ('Renata Figueiredo','renata.figueiredo@primecrm.local','renata.figueiredo','(21) 98220-3311','INACTIVE','LIGHT','COMERCIAL','COMERCIAL_RJ','EXECUTIVO',120,300),
    ('Anderson Peixoto','anderson.peixoto@primecrm.local','anderson.peixoto','(11) 99887-0022','BLOCKED','DARK','COMERCIAL','INSIDE_SALES','SDR',45,230),
    ('Tatiane Correia','tatiane.correia@primecrm.local','tatiane.correia','(11) 98002-5566','ACTIVE','LIGHT','TI','MARKETING_TIME','ESTAGIARIO',0,60),
    ('Felipe Marinho','felipe.marinho@primecrm.local','felipe.marinho','(11) 99771-8834','ACTIVE','DARK','TI','MARKETING_TIME','ESTAGIARIO',6,45)
) AS v(name, email, login, phone, status, theme, dep, team, pos, dias_ultimo_acesso, dias_cadastro)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.login = v.login);

-- Vinculo usuario -> perfil
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM (VALUES
    ('carla.menezes','Administrador'),
    ('rafael.duarte','Gerente'),
    ('juliana.prado','Gerente'),
    ('marcos.lima','Supervisor'),
    ('patricia.nogueira','Comercial'),
    ('diego.fontes','Comercial'),
    ('amanda.ribeiro','Comercial'),
    ('bruno.tavares','Comercial'),
    ('leticia.barbosa','Comercial'),
    ('thiago.moreira','Comercial'),
    ('fernanda.castro','Comercial'),
    ('gustavo.pereira','Comercial'),
    ('larissa.andrade','Comercial'),
    ('rodrigo.salles','Usuario Padrao'),
    ('camila.rocha','Atendimento'),
    ('vinicius.alencar','Atendimento'),
    ('beatriz.campos','Atendimento'),
    ('henrique.souza','Marketing'),
    ('isabela.martins','Marketing'),
    ('eduardo.ramos','Financeiro'),
    ('simone.vasques','Financeiro'),
    ('paulo.nunes','Comercial'),
    ('renata.figueiredo','Comercial'),
    ('anderson.peixoto','Usuario Padrao'),
    ('tatiane.correia','Usuario Padrao'),
    ('felipe.marinho','Usuario Padrao'),
    ('rafael.duarte','Supervisor'),
    ('juliana.prado','Comercial')
) AS v(login, role_name)
JOIN users u ON u.login = v.login
JOIN roles r ON r.name = v.role_name
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- ---------------------------------------------------------------------
-- 4. Funis (pipelines) e etapas
-- ---------------------------------------------------------------------
INSERT INTO pipelines (name, business_type, active)
SELECT v.name, v.business_type, v.active::boolean
FROM (VALUES
    ('Funil de Vendas B2B','Servicos',true),
    ('Funil de Vendas B2C','Varejo',true),
    ('Funil de Renovacao','Recorrencia',true),
    ('Funil de Parcerias','Canais',true),
    ('Funil de Licitacoes','Setor Publico',false)
) AS v(name, business_type, active)
WHERE NOT EXISTS (SELECT 1 FROM pipelines p WHERE p.name = v.name AND p.deleted_at IS NULL);

INSERT INTO pipeline_stages (pipeline_id, name, display_order, default_probability, sla_days, color, requires_loss_reason)
SELECT p.id, v.name, v.display_order::int, v.probability::numeric, v.sla::int, v.color, v.requires_loss::boolean
FROM (VALUES
    ('Funil de Vendas B2B','Prospeccao',1,10,5,'#71809A',false),
    ('Funil de Vendas B2B','Qualificacao',2,25,4,'#0EA5E9',false),
    ('Funil de Vendas B2B','Diagnostico',3,40,7,'#3B82F6',false),
    ('Funil de Vendas B2B','Proposta Enviada',4,60,5,'#1E5EFF',false),
    ('Funil de Vendas B2B','Negociacao',5,80,7,'#0B3C91',false),
    ('Funil de Vendas B2B','Ganho',6,100,NULL,'#22C55E',false),
    ('Funil de Vendas B2B','Perdido',7,0,NULL,'#EF4444',true),
    ('Funil de Vendas B2C','Contato Inicial',1,15,2,'#71809A',false),
    ('Funil de Vendas B2C','Apresentacao',2,45,2,'#3B82F6',false),
    ('Funil de Vendas B2C','Fechamento',3,85,1,'#1E5EFF',false),
    ('Funil de Vendas B2C','Ganho',4,100,NULL,'#22C55E',false),
    ('Funil de Vendas B2C','Perdido',5,0,NULL,'#EF4444',true),
    ('Funil de Renovacao','Contrato a Vencer',1,50,30,'#F59E0B',false),
    ('Funil de Renovacao','Contato de Renovacao',2,65,10,'#3B82F6',false),
    ('Funil de Renovacao','Proposta de Renovacao',3,80,7,'#1E5EFF',false),
    ('Funil de Renovacao','Renovado',4,100,NULL,'#22C55E',false),
    ('Funil de Renovacao','Churn',5,0,NULL,'#EF4444',true),
    ('Funil de Parcerias','Mapeamento',1,10,15,'#71809A',false),
    ('Funil de Parcerias','Reuniao de Alinhamento',2,35,10,'#0EA5E9',false),
    ('Funil de Parcerias','Acordo Comercial',3,70,20,'#1E5EFF',false),
    ('Funil de Parcerias','Parceria Ativa',4,100,NULL,'#22C55E',false),
    ('Funil de Licitacoes','Edital Publicado',1,20,10,'#71809A',false),
    ('Funil de Licitacoes','Habilitacao',2,45,15,'#3B82F6',false),
    ('Funil de Licitacoes','Julgamento',3,70,20,'#1E5EFF',false),
    ('Funil de Licitacoes','Homologado',4,100,NULL,'#22C55E',false),
    ('Funil de Licitacoes','Desclassificado',5,0,NULL,'#EF4444',true)
) AS v(pipeline_name, name, display_order, probability, sla, color, requires_loss)
JOIN pipelines p ON p.name = v.pipeline_name AND p.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM pipeline_stages ps WHERE ps.pipeline_id = p.id AND ps.name = v.name AND ps.deleted_at IS NULL
);

-- ---------------------------------------------------------------------
-- 5. Campos personalizados
-- ---------------------------------------------------------------------
INSERT INTO custom_fields (target_entity, field_key, label, field_type, options, required, display_order, active)
SELECT v.target_entity, v.field_key, v.label, v.field_type, v.options::jsonb, v.required::boolean, v.display_order::int, v.active::boolean
FROM (VALUES
    ('CLIENTE','faturamento_anual','Faturamento Anual','NUMBER',NULL,false,1,true),
    ('CLIENTE','numero_funcionarios','Numero de Funcionarios','NUMBER',NULL,false,2,true),
    ('CLIENTE','data_fundacao','Data de Fundacao','DATE',NULL,false,3,true),
    ('CLIENTE','regime_tributario','Regime Tributario','SELECT','{"1":"Simples Nacional","2":"Lucro Presumido","3":"Lucro Real","4":"MEI"}',false,4,true),
    ('CLIENTE','aceita_newsletter','Aceita Newsletter','BOOLEAN',NULL,false,5,true),
    ('CLIENTE','observacao_interna','Observacao Interna','TEXT',NULL,false,6,true),
    ('LEAD','budget_estimado','Budget Estimado','NUMBER',NULL,false,1,true),
    ('LEAD','prazo_decisao','Prazo de Decisao','SELECT','{"1":"Imediato","2":"30 dias","3":"90 dias","4":"Sem prazo definido"}',true,2,true),
    ('LEAD','canais_interesse','Canais de Interesse','MULTISELECT','{"1":"E-mail","2":"WhatsApp","3":"Telefone","4":"Presencial"}',false,3,true),
    ('LEAD','e_decisor','E o decisor','BOOLEAN',NULL,false,4,true),
    ('OPORTUNIDADE','concorrente','Concorrente','TEXT',NULL,false,1,true),
    ('OPORTUNIDADE','probabilidade_ajustada','Probabilidade Ajustada','NUMBER',NULL,false,2,true),
    ('OPORTUNIDADE','data_visita_tecnica','Data da Visita Tecnica','DATE',NULL,false,3,true),
    ('OPORTUNIDADE','modalidade','Modalidade','SELECT','{"1":"Presencial","2":"Remoto","3":"Hibrido"}',false,4,false)
) AS v(target_entity, field_key, label, field_type, options, required, display_order, active)
WHERE NOT EXISTS (
    SELECT 1 FROM custom_fields cf
    WHERE cf.target_entity = v.target_entity AND cf.field_key = v.field_key AND cf.deleted_at IS NULL
);

-- ---------------------------------------------------------------------
-- 6. Templates
-- ---------------------------------------------------------------------
INSERT INTO templates (type, name, subject, content, active)
SELECT v.type, v.name, v.subject, v.content, v.active::boolean
FROM (VALUES
    ('EMAIL','Boas-vindas ao cliente','Bem-vindo a Prime CRM, {{cliente}}!',
     'Ola {{contato}},' || chr(10) || chr(10) || 'E um prazer ter a {{cliente}} conosco. Seu executivo responsavel e {{responsavel}}.' || chr(10) || 'Qualquer duvida, e so responder este e-mail.' || chr(10) || chr(10) || 'Abracos,' || chr(10) || 'Equipe Prime CRM',true),
    ('EMAIL','Follow-up de proposta','Retomando nossa proposta - {{cliente}}',
     'Ola {{contato}},' || chr(10) || chr(10) || 'Passando para saber se conseguiu avaliar a proposta enviada em {{data_proposta}}.' || chr(10) || 'Fico a disposicao para ajustar o que for necessario.' || chr(10) || chr(10) || 'Atenciosamente,' || chr(10) || '{{responsavel}}',true),
    ('EMAIL','Aviso de vencimento de contrato','Seu contrato vence em {{dias}} dias',
     'Ola {{contato}},' || chr(10) || chr(10) || 'O contrato {{numero_contrato}} vence em {{data_vencimento}}.' || chr(10) || 'Vamos conversar sobre a renovacao?' || chr(10) || chr(10) || '{{responsavel}}',true),
    ('EMAIL','Reativacao de lead frio','Ainda faz sentido conversarmos, {{contato}}?',
     'Ola {{contato}},' || chr(10) || chr(10) || 'Faz um tempo que nao nos falamos. Continua no radar resolver {{dor_identificada}}?' || chr(10) || chr(10) || '{{responsavel}}',false),
    ('PROPOSAL','Proposta comercial padrao',NULL,
     'PROPOSTA COMERCIAL' || chr(10) || 'Cliente: {{cliente}}' || chr(10) || 'Validade: {{validade}}' || chr(10) || chr(10) || 'ESCOPO' || chr(10) || '{{escopo}}' || chr(10) || chr(10) || 'INVESTIMENTO' || chr(10) || '{{itens}}' || chr(10) || 'Total: {{valor_total}}' || chr(10) || chr(10) || 'CONDICOES DE PAGAMENTO' || chr(10) || '{{condicao_pagamento}}',true),
    ('PROPOSAL','Proposta de renovacao',NULL,
     'PROPOSTA DE RENOVACAO' || chr(10) || 'Cliente: {{cliente}}' || chr(10) || 'Contrato atual: {{numero_contrato}}' || chr(10) || 'Novo periodo: {{periodo}}' || chr(10) || 'Valor: {{valor_total}}',true),
    ('CONTRACT','Contrato de prestacao de servicos',NULL,
     'CONTRATO DE PRESTACAO DE SERVICOS' || chr(10) || chr(10) || 'CONTRATANTE: {{cliente}}, inscrita no CNPJ {{cnpj}}.' || chr(10) || 'CONTRATADA: Prime CRM Ltda.' || chr(10) || chr(10) || 'OBJETO: {{objeto}}' || chr(10) || 'VIGENCIA: {{data_inicio}} a {{data_fim}}' || chr(10) || 'VALOR: {{valor}}',true),
    ('CONTRACT','Termo aditivo',NULL,
     'TERMO ADITIVO AO CONTRATO {{numero_contrato}}' || chr(10) || chr(10) || 'Fica alterada a clausula {{clausula}} conforme segue: {{alteracao}}',true),
    ('WHATSAPP','Primeiro contato',NULL,
     'Oi {{contato}}, tudo bem? Aqui e {{responsavel}} da Prime CRM. Vi seu interesse em {{interesse}} e queria entender melhor sua necessidade. Pode falar agora?',true),
    ('WHATSAPP','Lembrete de reuniao',NULL,
     'Oi {{contato}}! Passando para lembrar da nossa reuniao em {{data_hora}}. Link: {{link}}. Ate la!',true),
    ('WHATSAPP','Pos-venda',NULL,
     'Oi {{contato}}, tudo certo com {{produto}}? Qualquer coisa e so chamar por aqui.',true)
) AS v(type, name, subject, content, active)
WHERE NOT EXISTS (SELECT 1 FROM templates t WHERE t.name = v.name AND t.deleted_at IS NULL);

-- ---------------------------------------------------------------------
-- 7. Feriados (nacionais 2026/2027 + regionais)
-- ---------------------------------------------------------------------
INSERT INTO holidays (holiday_date, name, national, active)
SELECT v.holiday_date::date, v.name, v.national::boolean, true
FROM (VALUES
    ('2026-01-01','Confraternizacao Universal',true),
    ('2026-02-16','Carnaval',true),
    ('2026-02-17','Carnaval',true),
    ('2026-04-03','Sexta-feira Santa',true),
    ('2026-04-21','Tiradentes',true),
    ('2026-05-01','Dia do Trabalho',true),
    ('2026-06-04','Corpus Christi',true),
    ('2026-09-07','Independencia do Brasil',true),
    ('2026-10-12','Nossa Senhora Aparecida',true),
    ('2026-11-02','Finados',true),
    ('2026-11-15','Proclamacao da Republica',true),
    ('2026-11-20','Consciencia Negra',true),
    ('2026-12-25','Natal',true),
    ('2027-01-01','Confraternizacao Universal',true),
    ('2027-02-08','Carnaval',true),
    ('2027-02-09','Carnaval',true),
    ('2027-03-26','Sexta-feira Santa',true),
    ('2027-04-21','Tiradentes',true),
    ('2027-05-01','Dia do Trabalho',true),
    ('2027-09-07','Independencia do Brasil',true),
    ('2027-10-12','Nossa Senhora Aparecida',true),
    ('2027-11-02','Finados',true),
    ('2027-11-15','Proclamacao da Republica',true),
    ('2027-12-25','Natal',true),
    ('2026-01-25','Aniversario de Sao Paulo',false),
    ('2026-04-23','Sao Jorge (RJ)',false),
    ('2026-07-09','Revolucao Constitucionalista (SP)',false),
    ('2026-09-20','Revolucao Farroupilha (RS)',false),
    ('2026-12-08','Nossa Senhora da Conceicao (BA)',false),
    ('2026-12-24','Vespera de Natal (ponto facultativo)',false),
    ('2026-12-31','Vespera de Ano Novo (ponto facultativo)',false)
) AS v(holiday_date, name, national)
WHERE NOT EXISTS (
    SELECT 1 FROM holidays h WHERE h.holiday_date = v.holiday_date::date AND h.name = v.name AND h.deleted_at IS NULL
);

-- ---------------------------------------------------------------------
-- 8. Configuracoes gerais
-- ---------------------------------------------------------------------
UPDATE system_settings SET setting_value = v.value, updated_at = now()
FROM (VALUES
    ('default_currency','BRL'),
    ('default_timezone','America/Sao_Paulo'),
    ('date_format','dd/MM/yyyy'),
    ('business_days','SEG,TER,QUA,QUI,SEX')
) AS v(key, value)
WHERE system_settings.setting_key = v.key;

-- ---------------------------------------------------------------------
-- 9. Historico de auditoria (da cara de sistema em uso)
-- ---------------------------------------------------------------------
INSERT INTO audit_log (entity_name, entity_id, action, changes, user_id, user_email, ip_address, user_agent, tenant_id, created_at)
SELECT
    e.entity_name,
    gen_random_uuid(),
    a.action,
    CASE a.action
        WHEN 'CREATE' THEN jsonb_build_object('name', 'Registro ' || g, 'active', true)
        WHEN 'UPDATE' THEN jsonb_build_object('name', jsonb_build_object('old', 'Registro ' || g, 'new', 'Registro ' || g || ' (revisado)'))
        ELSE jsonb_build_object('name', 'Registro ' || g)
    END,
    u.id,
    u.email,
    '10.0.' || (1 + (g % 6)) || '.' || (10 + (g % 200)),
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/151.0 Safari/537.36',
    '00000000-0000-0000-0000-000000000001',
    now() - ((g % 90) * interval '1 day') - ((g % 24) * interval '1 hour') - ((g % 60) * interval '1 minute')
FROM generate_series(1, 70) g
CROSS JOIN (VALUES ('DomainValue'),('Pipeline'),('PipelineStage'),('User'),('Role'),('Template'),('Holiday'),('CustomField')) AS e(entity_name)
CROSS JOIN LATERAL (
    SELECT (ARRAY['CREATE','UPDATE','UPDATE','UPDATE','DELETE'])[1 + ((g + length(e.entity_name)) % 5)] AS action
) a
CROSS JOIN LATERAL (
    SELECT id, email FROM users
    WHERE deleted_at IS NULL
    ORDER BY md5(g::text || e.entity_name || id::text)
    LIMIT 1
) u
WHERE (SELECT count(*) FROM audit_log) < 100;

-- ---------------------------------------------------------------------
-- Resumo
-- ---------------------------------------------------------------------
SELECT 'domain_values' AS tabela, count(*) AS registros FROM domain_values WHERE deleted_at IS NULL
UNION ALL SELECT 'users',            count(*) FROM users WHERE deleted_at IS NULL
UNION ALL SELECT 'roles',            count(*) FROM roles WHERE deleted_at IS NULL
UNION ALL SELECT 'user_roles',       count(*) FROM user_roles
UNION ALL SELECT 'role_permissions', count(*) FROM role_permissions
UNION ALL SELECT 'pipelines',        count(*) FROM pipelines WHERE deleted_at IS NULL
UNION ALL SELECT 'pipeline_stages',  count(*) FROM pipeline_stages WHERE deleted_at IS NULL
UNION ALL SELECT 'custom_fields',    count(*) FROM custom_fields WHERE deleted_at IS NULL
UNION ALL SELECT 'templates',        count(*) FROM templates WHERE deleted_at IS NULL
UNION ALL SELECT 'holidays',         count(*) FROM holidays WHERE deleted_at IS NULL
UNION ALL SELECT 'audit_log',        count(*) FROM audit_log
ORDER BY tabela;

COMMIT;

-- =====================================================================
-- LIMPEZA (opcional) — remove somente o que este script criou.
-- Nao apaga o usuario admin nem o perfil Administrador originais.
-- Rode manualmente se quiser voltar ao estado limpo:
-- =====================================================================
-- BEGIN;
-- DELETE FROM audit_log;
-- DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE login <> 'admin');
-- DELETE FROM users WHERE login <> 'admin';
-- DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE name <> 'Administrador');
-- DELETE FROM roles WHERE name <> 'Administrador';
-- DELETE FROM pipeline_stages;
-- DELETE FROM pipelines;
-- DELETE FROM custom_fields;
-- DELETE FROM templates;
-- DELETE FROM holidays;
-- COMMIT;
