-- =====================================================================
-- Prime CRM — massa de dados do Nucleo Comercial (Fase 2)
--
-- Gera clientes (PF e PJ), contatos, leads e oportunidades distribuidas
-- pelas etapas dos funis, com historico de movimentacao.
--
-- PRE-REQUISITO: rode antes o scripts/demo-data.sql — este script depende
-- dos usuarios, dos valores de dominio e dos funis criados la.
--
-- Uso:  psql -U postgres -h localhost -d primecrm -f scripts/demo-data-commercial.sql
--
-- Seguro rodar mais de uma vez: nao duplica (checa volume antes de inserir).
-- Os documentos (CPF/CNPJ) sao gerados com digito verificador valido, para
-- que os registros passem na validacao do formulario da tela.
-- =====================================================================

BEGIN;

SELECT setseed(0.42);

-- ---------------------------------------------------------------------
-- Funcoes auxiliares (temporarias: somem ao fim da sessao)
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION pg_temp.gen_cpf(seed bigint) RETURNS text AS $$
DECLARE
    base int[] := ARRAY[]::int[];
    i int; soma int := 0; resto int; d1 int; d2 int; s text;
BEGIN
    s := lpad(((seed * 7919 + 13) % 1000000000)::text, 9, '0');
    FOR i IN 1..9 LOOP base := base || substr(s, i, 1)::int; END LOOP;

    soma := 0;
    FOR i IN 1..9 LOOP soma := soma + base[i] * (11 - i); END LOOP;
    resto := soma % 11;
    d1 := CASE WHEN resto < 2 THEN 0 ELSE 11 - resto END;

    base := base || d1;
    soma := 0;
    FOR i IN 1..10 LOOP soma := soma + base[i] * (12 - i); END LOOP;
    resto := soma % 11;
    d2 := CASE WHEN resto < 2 THEN 0 ELSE 11 - resto END;

    RETURN s || d1::text || d2::text;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION pg_temp.gen_cnpj(seed bigint) RETURNS text AS $$
DECLARE
    base int[] := ARRAY[]::int[];
    p1 int[] := ARRAY[5,4,3,2,9,8,7,6,5,4,3,2];
    p2 int[] := ARRAY[6,5,4,3,2,9,8,7,6,5,4,3,2];
    i int; soma int := 0; resto int; d1 int; d2 int; s text;
BEGIN
    s := lpad(((seed * 6151 + 271) % 100000000)::text, 8, '0') || '0001';
    FOR i IN 1..12 LOOP base := base || substr(s, i, 1)::int; END LOOP;

    soma := 0;
    FOR i IN 1..12 LOOP soma := soma + base[i] * p1[i]; END LOOP;
    resto := soma % 11;
    d1 := CASE WHEN resto < 2 THEN 0 ELSE 11 - resto END;

    base := base || d1;
    soma := 0;
    FOR i IN 1..13 LOOP soma := soma + base[i] * p2[i]; END LOOP;
    resto := soma % 11;
    d2 := CASE WHEN resto < 2 THEN 0 ELSE 11 - resto END;

    RETURN s || d1::text || d2::text;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ---------------------------------------------------------------------
-- 1. Clientes — 320 registros (PJ e PF)
-- ---------------------------------------------------------------------
INSERT INTO customers (
    name, trade_name, person_type, document, state_registration,
    client_type_id, segment_id, activity_branch_id, category_id, origin_id, status_id,
    owner_user_id, team_id, phone, mobile, email, financial_email, website,
    zip_code, street, number, district, city, state, country,
    birth_date, last_contact_at, next_contact_at, credit_limit, payment_terms,
    health_score, notes, active, created_at
)
SELECT
    CASE WHEN g % 5 = 0
         THEN (ARRAY['Ana Beatriz','Carlos Eduardo','Mariana','Roberto','Fernanda','Paulo','Juliana','Marcelo','Patricia','Ricardo'])[1 + (g % 10)]
              || ' ' || (ARRAY['Silva','Souza','Oliveira','Pereira','Costa','Almeida','Ferreira','Rodrigues','Martins','Barbosa'])[1 + ((g / 3) % 10)]
         ELSE (ARRAY['Alfa','Nova','Prime','Global','Terra','Vetor','Nexus','Orion','Delta','Horizonte','Atlas','Vertice'])[1 + (g % 12)]
              || ' ' || (ARRAY['Industria','Comercio','Servicos','Tecnologia','Logistica','Solucoes','Distribuidora','Engenharia'])[1 + ((g / 5) % 8)]
              || ' ' || (ARRAY['Ltda','S.A.','ME','EIRELI'])[1 + ((g / 7) % 4)]
    END,
    CASE WHEN g % 5 = 0 THEN NULL
         ELSE (ARRAY['Alfa','Nova','Prime','Global','Terra','Vetor','Nexus','Orion','Delta','Horizonte','Atlas','Vertice'])[1 + (g % 12)]
    END,
    CASE WHEN g % 5 = 0 THEN 'FISICA' ELSE 'JURIDICA' END,
    CASE WHEN g % 5 = 0 THEN pg_temp.gen_cpf(g) ELSE pg_temp.gen_cnpj(g) END,
    CASE WHEN g % 5 = 0 THEN NULL ELSE 'ISENTO' END,
    (SELECT dv.id FROM domain_values dv JOIN domain_types dt ON dt.id = dv.domain_type_id
      WHERE dt.code = 'CLIENT_TYPE' AND dv.code = CASE WHEN g % 5 = 0 THEN 'PF' ELSE 'PJ' END),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='MARKET_SEGMENT')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 12) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='ACTIVITY_BRANCH')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 10) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='CATEGORY')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 5) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='LEAD_ORIGIN')
       AND dv.deleted_at IS NULL AND dv.active ORDER BY dv.display_order OFFSET (g % 9) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='GENERIC_STATUS')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 4) LIMIT 1),
    (SELECT id FROM users u WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE'
       ORDER BY u.login OFFSET (g % 20) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='TEAM')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 7) LIMIT 1),
    '(' || (ARRAY['11','21','31','41','51','61'])[1 + (g % 6)] || ') ' || (3000 + (g % 6000))::text || '-' || lpad((g % 10000)::text, 4, '0'),
    '(' || (ARRAY['11','21','31','41','51','61'])[1 + (g % 6)] || ') 9' || (8000 + (g % 1999))::text || '-' || lpad((g % 10000)::text, 4, '0'),
    'contato' || g || '@' || lower((ARRAY['alfa','nova','prime','global','terra','vetor'])[1 + (g % 6)]) || '.com.br',
    'financeiro' || g || '@' || lower((ARRAY['alfa','nova','prime','global','terra','vetor'])[1 + (g % 6)]) || '.com.br',
    CASE WHEN g % 5 = 0 THEN NULL ELSE 'https://www.' || lower((ARRAY['alfa','nova','prime','global','terra','vetor'])[1 + (g % 6)]) || g || '.com.br' END,
    lpad(((g * 137) % 99999)::text, 5, '0') || '-' || lpad((g % 1000)::text, 3, '0'),
    (ARRAY['Av. Paulista','Rua das Flores','Av. Brasil','Rua XV de Novembro','Av. Ipiranga','Rua Sete de Setembro','Av. Atlantica'])[1 + (g % 7)],
    (100 + (g % 3000))::text,
    (ARRAY['Centro','Jardins','Bela Vista','Moema','Batel','Savassi','Boa Viagem'])[1 + (g % 7)],
    (ARRAY['Sao Paulo','Rio de Janeiro','Belo Horizonte','Curitiba','Porto Alegre','Brasilia','Recife','Salvador','Campinas','Florianopolis'])[1 + (g % 10)],
    (ARRAY['SP','RJ','MG','PR','RS','DF','PE','BA','SP','SC'])[1 + (g % 10)],
    'Brasil',
    (DATE '1970-01-01' + ((g * 97) % 16000))::date,
    now() - ((g % 60) * interval '1 day'),
    CASE WHEN g % 3 = 0 THEN now() + ((g % 30) * interval '1 day') ELSE NULL END,
    ((g % 20) + 1) * 5000::numeric,
    (ARRAY['A vista','30 dias','30/60 dias','30/60/90 dias','Boleto 15 dias'])[1 + (g % 5)],
    40 + (g % 61),
    CASE WHEN g % 7 = 0 THEN 'Cliente com historico de compras recorrentes.' ELSE NULL END,
    (g % 17 <> 0),
    now() - ((g % 400) * interval '1 day')
FROM generate_series(1, 320) g
WHERE (SELECT count(*) FROM customers WHERE deleted_at IS NULL) < 50;

-- ---------------------------------------------------------------------
-- 2. Contatos — 1 a 3 por cliente pessoa juridica
-- ---------------------------------------------------------------------
INSERT INTO contacts (customer_id, name, position_title, department_id, email, phone, mobile,
                      birth_date, primary_contact, decision_maker, active, created_at)
SELECT
    c.id,
    (ARRAY['Ana','Bruno','Carla','Daniel','Eduarda','Felipe','Gabriela','Henrique','Isabela','Joao','Karina','Lucas'])[1 + ((c.rn + n) % 12)]
      || ' ' || (ARRAY['Alves','Barros','Cardoso','Dias','Esteves','Freitas','Gomes','Henriques','Ivo','Jardim'])[1 + ((c.rn * n) % 10)],
    (ARRAY['Diretor','Gerente de Compras','Coordenador','Analista','Supervisor','Socio'])[1 + ((c.rn + n) % 6)],
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='DEPARTMENT')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET ((c.rn + n) % 7) LIMIT 1),
    'contato' || c.rn || '.' || n || '@empresa.com.br',
    '(' || (ARRAY['11','21','31','41','51'])[1 + (c.rn % 5)] || ') ' || (3000 + (c.rn % 6000))::text || '-' || lpad(((c.rn * n) % 10000)::text, 4, '0'),
    '(' || (ARRAY['11','21','31','41','51'])[1 + (c.rn % 5)] || ') 9' || (8000 + (c.rn % 1999))::text || '-' || lpad(((c.rn * n) % 10000)::text, 4, '0'),
    (DATE '1975-01-01' + (((c.rn * 53 + n) % 14000))::int)::date,
    (n = 1),
    (n = 1 OR (c.rn + n) % 4 = 0),
    true,
    now() - ((c.rn % 300) * interval '1 day')
FROM (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM customers WHERE deleted_at IS NULL AND person_type = 'JURIDICA'
) c
CROSS JOIN generate_series(1, 3) n
WHERE n <= 1 + (c.rn % 3)
  AND (SELECT count(*) FROM contacts WHERE deleted_at IS NULL) < 50;

-- ---------------------------------------------------------------------
-- 3. Leads — 260 registros, parte deles ja convertidos
-- ---------------------------------------------------------------------
INSERT INTO leads (name, company_name, contact_name, email, phone, mobile,
                   origin_id, status_id, priority_id, campaign, owner_user_id,
                   pipeline_id, stage_id, probability, estimated_value, expected_close_date,
                   qualification_score, converted_customer_id, converted_at, notes, active, created_at)
SELECT
    (ARRAY['Ana','Bruno','Carla','Diego','Elaine','Fabio','Giovana','Hugo','Ines','Jorge'])[1 + (g % 10)]
      || ' ' || (ARRAY['Moraes','Nunes','Oliveira','Prado','Queiroz','Ramos','Santana','Teixeira'])[1 + ((g / 3) % 8)],
    (ARRAY['Construtora','Distribuidora','Clinica','Escola','Transportadora','Consultoria','Agencia','Industria'])[1 + (g % 8)]
      || ' ' || (ARRAY['Aurora','Bandeirantes','Central','Diamante','Estrela','Fenix','Guardia','Horizonte'])[1 + ((g / 4) % 8)],
    (ARRAY['Ana Paula','Carlos','Marina','Roberto','Sandra','Tiago'])[1 + (g % 6)],
    'lead' || g || '@' || lower((ARRAY['aurora','bandeirantes','central','diamante','estrela','fenix'])[1 + (g % 6)]) || '.com.br',
    '(' || (ARRAY['11','21','31','41','51','62'])[1 + (g % 6)] || ') ' || (3000 + (g % 6000))::text || '-' || lpad((g % 10000)::text, 4, '0'),
    '(' || (ARRAY['11','21','31','41','51','62'])[1 + (g % 6)] || ') 9' || (8000 + (g % 1999))::text || '-' || lpad((g % 10000)::text, 4, '0'),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='LEAD_ORIGIN')
       AND dv.deleted_at IS NULL AND dv.active ORDER BY dv.display_order OFFSET (g % 9) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='GENERIC_STATUS')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 4) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='PRIORITY')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 4) LIMIT 1),
    (ARRAY['Google Ads - Institucional','Meta Ads - Remarketing','Feira do Setor 2026','Webinar Automacao','Indicacao de Parceiro','Inbound - E-book'])[1 + (g % 6)],
    (SELECT id FROM users u WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE' ORDER BY u.login OFFSET (g % 20) LIMIT 1),
    p.pipeline_id,
    p.stage_id,
    p.probability,
    ((g % 40) + 3) * 2500::numeric,
    (CURRENT_DATE + ((g % 120) - 30))::date,
    30 + (g % 71),
    NULL,
    NULL,
    CASE WHEN g % 11 = 0 THEN 'Lead pediu retorno apos o proximo trimestre.' ELSE NULL END,
    (g % 23 <> 0),
    now() - ((g % 180) * interval '1 day')
FROM generate_series(1, 260) g
CROSS JOIN LATERAL (
    SELECT ps.pipeline_id, ps.id AS stage_id, ps.default_probability AS probability
    FROM pipeline_stages ps
    JOIN pipelines pl ON pl.id = ps.pipeline_id AND pl.deleted_at IS NULL AND pl.active
    WHERE ps.deleted_at IS NULL AND ps.default_probability NOT IN (0, 100)
    ORDER BY md5(g::text || ps.id::text)
    LIMIT 1
) p
WHERE (SELECT count(*) FROM leads WHERE deleted_at IS NULL) < 50;

-- Marca 1 em cada 5 leads como convertido, vinculando a um cliente existente
WITH leads_numerados AS (
    SELECT id, created_at, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM leads
    WHERE deleted_at IS NULL AND converted_customer_id IS NULL
), clientes_numerados AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM customers WHERE deleted_at IS NULL
), para_converter AS (
    SELECT l.id AS lead_id, l.created_at AS lead_criado_em, c.id AS customer_id
    FROM leads_numerados l
    JOIN clientes_numerados c ON c.rn = l.rn
    WHERE l.rn % 5 = 0
      AND (SELECT count(*) FROM leads WHERE converted_customer_id IS NOT NULL AND deleted_at IS NULL) = 0
)
UPDATE leads
SET converted_customer_id = pc.customer_id,
    converted_at = pc.lead_criado_em + interval '9 days',
    status_id = (SELECT id FROM domain_values dv
                  WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='GENERIC_STATUS')
                    AND dv.code = 'CONCLUIDO'),
    updated_at = now()
FROM para_converter pc
WHERE leads.id = pc.lead_id;

-- ---------------------------------------------------------------------
-- 4. Oportunidades — 420 espalhadas pelas etapas dos funis
-- ---------------------------------------------------------------------
INSERT INTO opportunities (title, customer_id, contact_id, pipeline_id, stage_id, amount, probability,
                           owner_user_id, team_id, opened_at, expected_close_date, closed_at,
                           outcome, win_reason_id, loss_reason_id, competitor, notes, created_at)
SELECT
    (ARRAY['Implantacao','Renovacao','Expansao','Projeto','Contrato','Fornecimento'])[1 + (g % 6)]
      || ' ' || (ARRAY['CRM','ERP','Consultoria','Suporte','Licencas','Treinamento','Infraestrutura'])[1 + ((g / 2) % 7)]
      || ' - ' || cust.name,
    cust.id,
    (SELECT ct.id FROM contacts ct WHERE ct.customer_id = cust.id AND ct.deleted_at IS NULL ORDER BY ct.primary_contact DESC LIMIT 1),
    st.pipeline_id,
    st.stage_id,
    ((g % 60) + 4) * 3500::numeric,
    st.probability,
    (SELECT id FROM users u WHERE u.deleted_at IS NULL AND u.status = 'ACTIVE' ORDER BY u.login OFFSET (g % 20) LIMIT 1),
    (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='TEAM')
       AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 7) LIMIT 1),
    now() - ((g % 150) * interval '1 day'),
    (CURRENT_DATE + ((g % 90) - 20))::date,
    CASE WHEN st.probability IN (0, 100) THEN now() - ((g % 40) * interval '1 day') ELSE NULL END,
    CASE WHEN st.probability = 100 THEN 'WON' WHEN st.probability = 0 THEN 'LOST' ELSE 'OPEN' END,
    CASE WHEN st.probability = 100 THEN
        (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='WIN_REASON')
           AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 6) LIMIT 1)
    ELSE NULL END,
    CASE WHEN st.probability = 0 THEN
        (SELECT id FROM domain_values dv WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='LOSS_REASON')
           AND dv.deleted_at IS NULL ORDER BY dv.display_order OFFSET (g % 7) LIMIT 1)
    ELSE NULL END,
    CASE WHEN g % 4 = 0 THEN (ARRAY['Concorrente A','Concorrente B','Solucao interna','Planilha'])[1 + (g % 4)] ELSE NULL END,
    CASE WHEN g % 9 = 0 THEN 'Negociacao envolve desconto por volume.' ELSE NULL END,
    now() - ((g % 150) * interval '1 day')
FROM generate_series(1, 420) g
CROSS JOIN LATERAL (
    SELECT c.id, c.name FROM customers c
    WHERE c.deleted_at IS NULL AND c.active
    ORDER BY md5(g::text || c.id::text) LIMIT 1
) cust
CROSS JOIN LATERAL (
    SELECT ps.pipeline_id, ps.id AS stage_id, ps.default_probability AS probability
    FROM pipeline_stages ps
    JOIN pipelines pl ON pl.id = ps.pipeline_id AND pl.deleted_at IS NULL AND pl.active
    WHERE ps.deleted_at IS NULL
    ORDER BY md5(g::text || 'st' || ps.id::text)
    LIMIT 1
) st
WHERE (SELECT count(*) FROM opportunities WHERE deleted_at IS NULL) < 50;

-- ---------------------------------------------------------------------
-- 5. Historico de movimentacao: da etapa 1 ate a etapa atual
-- ---------------------------------------------------------------------
INSERT INTO opportunity_stage_history (opportunity_id, from_stage_id, to_stage_id, moved_by_user_id,
                                       moved_at, days_in_previous_stage, note)
SELECT
    o.id,
    prev.id,
    cur.id,
    o.owner_user_id,
    o.opened_at + (cur.display_order * interval '6 days'),
    CASE WHEN prev.id IS NULL THEN NULL ELSE 3 + ((cur.display_order * 7) % 12) END,
    CASE WHEN prev.id IS NULL THEN 'Oportunidade criada' ELSE NULL END
FROM opportunities o
JOIN pipeline_stages atual ON atual.id = o.stage_id
JOIN pipeline_stages cur ON cur.pipeline_id = o.pipeline_id
                        AND cur.deleted_at IS NULL
                        AND cur.display_order <= atual.display_order
LEFT JOIN pipeline_stages prev ON prev.pipeline_id = o.pipeline_id
                              AND prev.deleted_at IS NULL
                              AND prev.display_order = cur.display_order - 1
WHERE o.deleted_at IS NULL
  AND (SELECT count(*) FROM opportunity_stage_history) < 50;

-- ---------------------------------------------------------------------
-- 6. Tags de clientes e leads
-- ---------------------------------------------------------------------
INSERT INTO customer_tags (customer_id, domain_value_id)
SELECT c.id, t.id
FROM (SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM customers WHERE deleted_at IS NULL) c
CROSS JOIN LATERAL (
    SELECT dv.id FROM domain_values dv
    WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='TAG') AND dv.deleted_at IS NULL
    ORDER BY md5(c.rn::text || dv.id::text) LIMIT 1 + (c.rn % 2)
) t
WHERE c.rn % 2 = 0
  AND NOT EXISTS (SELECT 1 FROM customer_tags ct WHERE ct.customer_id = c.id AND ct.domain_value_id = t.id);

INSERT INTO lead_tags (lead_id, domain_value_id)
SELECT l.id, t.id
FROM (SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM leads WHERE deleted_at IS NULL) l
CROSS JOIN LATERAL (
    SELECT dv.id FROM domain_values dv
    WHERE dv.domain_type_id = (SELECT id FROM domain_types WHERE code='TAG') AND dv.deleted_at IS NULL
    ORDER BY md5(l.rn::text || 'lead' || dv.id::text) LIMIT 1
) t
WHERE l.rn % 3 = 0
  AND NOT EXISTS (SELECT 1 FROM lead_tags lt WHERE lt.lead_id = l.id AND lt.domain_value_id = t.id);

-- ---------------------------------------------------------------------
-- Resumo
-- ---------------------------------------------------------------------
SELECT 'customers' AS tabela, count(*) AS registros FROM customers WHERE deleted_at IS NULL
UNION ALL SELECT 'contacts',                  count(*) FROM contacts WHERE deleted_at IS NULL
UNION ALL SELECT 'leads',                     count(*) FROM leads WHERE deleted_at IS NULL
UNION ALL SELECT 'leads convertidos',         count(*) FROM leads WHERE converted_customer_id IS NOT NULL
UNION ALL SELECT 'opportunities',             count(*) FROM opportunities WHERE deleted_at IS NULL
UNION ALL SELECT '  em aberto',               count(*) FROM opportunities WHERE outcome = 'OPEN' AND deleted_at IS NULL
UNION ALL SELECT '  ganhas',                  count(*) FROM opportunities WHERE outcome = 'WON' AND deleted_at IS NULL
UNION ALL SELECT '  perdidas',                count(*) FROM opportunities WHERE outcome = 'LOST' AND deleted_at IS NULL
UNION ALL SELECT 'stage_history',             count(*) FROM opportunity_stage_history
UNION ALL SELECT 'customer_tags',             count(*) FROM customer_tags
UNION ALL SELECT 'lead_tags',                 count(*) FROM lead_tags
ORDER BY tabela;

COMMIT;

-- =====================================================================
-- LIMPEZA (opcional) — remove apenas os dados comerciais:
-- =====================================================================
-- BEGIN;
-- DELETE FROM opportunity_stage_history;
-- DELETE FROM opportunities;
-- DELETE FROM lead_tags;
-- DELETE FROM leads;
-- DELETE FROM customer_tags;
-- DELETE FROM contacts;
-- DELETE FROM customers;
-- COMMIT;