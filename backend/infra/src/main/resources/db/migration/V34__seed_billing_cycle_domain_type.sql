-- Fase 4 - Contratos: ciclo de faturamento reaproveita o engine domain_types/domain_values,
-- no mesmo espirito de UNIT_OF_MEASURE (Produtos) - varia por operacao e nao deve ser fixo no codigo.
INSERT INTO domain_types (code, label, supports_color, supports_icon, system_defined)
SELECT 'BILLING_CYCLE', 'Ciclo de Faturamento', false, false, true
WHERE NOT EXISTS (SELECT 1 FROM domain_types WHERE code = 'BILLING_CYCLE');

INSERT INTO domain_values (domain_type_id, code, name, display_order)
SELECT dt.id, v.code, v.name, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('UNICO', 'Pagamento Unico', 1),
    ('MENSAL', 'Mensal', 2),
    ('TRIMESTRAL', 'Trimestral', 3),
    ('SEMESTRAL', 'Semestral', 4),
    ('ANUAL', 'Anual', 5)
) AS v(code, name, display_order)
WHERE dt.code = 'BILLING_CYCLE'
  AND NOT EXISTS (
      SELECT 1 FROM domain_values dv WHERE dv.domain_type_id = dt.id AND dv.code = v.code
  );
