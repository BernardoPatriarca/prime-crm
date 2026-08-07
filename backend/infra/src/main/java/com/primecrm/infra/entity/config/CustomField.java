package com.primecrm.infra.entity.config;

import com.primecrm.infra.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "custom_fields")
public class CustomField extends BaseEntity {

    @Column(name = "target_entity", nullable = false, length = 60)
    private String targetEntity;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private FieldType fieldType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> options;

    @Column(nullable = false)
    private boolean required = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;
}
