-- Fase 2 - Nucleo Comercial: codigo legivel sequencial de clientes, leads e oportunidades.
-- O valor e gerado pelo proprio banco (DEFAULT + sequence) e nao pela aplicacao por dois motivos:
--   1. scripts de massa de dados inserem milhares de linhas em SQL puro e recebem o codigo de graca;
--   2. nextval() e atomico, o que elimina a corrida de concorrencia de um "select max + 1" no Java.
-- No lado JPA a coluna e read-only (@Generated(event = INSERT) + insertable/updatable = false):
-- a API nunca aceita "code" em request de criacao ou edicao.
-- As sequences comecam em 1000 apenas por estetica (codigos com 6 digitos desde o primeiro registro).

CREATE SEQUENCE customer_code_seq START 1000;
CREATE SEQUENCE lead_code_seq START 1000;
CREATE SEQUENCE opportunity_code_seq START 1000;

ALTER TABLE customers
    ALTER COLUMN code SET DEFAULT ('CLI-' || lpad(nextval('customer_code_seq')::text, 6, '0'));

ALTER TABLE leads
    ALTER COLUMN code SET DEFAULT ('LEAD-' || lpad(nextval('lead_code_seq')::text, 6, '0'));

ALTER TABLE opportunities
    ALTER COLUMN code SET DEFAULT ('OPO-' || lpad(nextval('opportunity_code_seq')::text, 6, '0'));
