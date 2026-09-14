-- Fase 4 - Produtos: unidade de medida reaproveita o engine domain_types/domain_values
-- em vez de um enum fixo no codigo, ja que varia muito entre comercio/industria/servicos.
INSERT INTO domain_types (code, label, supports_color, supports_icon, system_defined)
SELECT 'UNIT_OF_MEASURE', 'Unidade de Medida', false, false, true
WHERE NOT EXISTS (SELECT 1 FROM domain_types WHERE code = 'UNIT_OF_MEASURE');

INSERT INTO domain_values (domain_type_id, code, name, display_order)
SELECT dt.id, v.code, v.name, v.display_order
FROM domain_types dt
CROSS JOIN (VALUES
    ('UN', 'Unidade', 1),
    ('PC', 'Peca', 2),
    ('CX', 'Caixa', 3),
    ('KG', 'Quilograma', 4),
    ('L', 'Litro', 5),
    ('M', 'Metro', 6),
    ('M2', 'Metro Quadrado', 7),
    ('M3', 'Metro Cubico', 8),
    ('H', 'Hora', 9)
) AS v(code, name, display_order)
WHERE dt.code = 'UNIT_OF_MEASURE'
  AND NOT EXISTS (
      SELECT 1 FROM domain_values dv WHERE dv.domain_type_id = dt.id AND dv.code = v.code
  );
