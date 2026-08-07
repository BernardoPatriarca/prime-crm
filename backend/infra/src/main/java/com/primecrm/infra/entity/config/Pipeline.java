package com.primecrm.infra.entity.config;

import com.primecrm.infra.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pipelines")
public class Pipeline extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(nullable = false)
    private boolean active = true;
}
