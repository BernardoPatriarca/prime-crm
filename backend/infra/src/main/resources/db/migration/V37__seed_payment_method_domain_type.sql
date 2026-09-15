-- Fase 5 - Financeiro: forma de pagamento reaproveita o engine domain_types/domain_values,
-- mesmo raciocinio ja usado em UNIT_OF_MEASURE (Produtos) e BILLING_CYCLE (Contratos).
INSERT INTO domain_types (code, label, supports_color, supports_icon, system_defined)
SELECT 'PAYMENT_METHOD', 'Forma de Pagamento', false, true, true
WHERE NOT EXISTS (SELECT 1 FROM domain_types WHERE code = 'PAYMENT_METHOD');

INSERT INTO domain_values (domain_type_id, code, name, icon, display_order)
SELECT dt.id, v.code, v.name, v.icon, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('BOLETO', 'Boleto', 'pi-file', 1),
    ('PIX', 'Pix', 'pi-bolt', 2),
    ('CARTAO_CREDITO', 'Cartao de Credito', 'pi-credit-card', 3),
    ('TRANSFERENCIA', 'Transferencia Bancaria', 'pi-wallet', 4),
    ('DINHEIRO', 'Dinheiro', 'pi-money-bill', 5)
) AS v(code, name, icon, display_order)
WHERE dt.code = 'PAYMENT_METHOD'
  AND NOT EXISTS (
      SELECT 1 FROM domain_values dv WHERE dv.domain_type_id = dt.id AND dv.code = v.code
  );
