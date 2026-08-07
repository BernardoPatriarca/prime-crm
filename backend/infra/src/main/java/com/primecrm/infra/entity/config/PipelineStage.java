package com.primecrm.infra.entity.config;

import com.primecrm.infra.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pipeline_stages")
public class PipelineStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "default_probability", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultProbability = BigDecimal.ZERO;

    @Column(name = "sla_days")
    private Integer slaDays;

    @Column(length = 7)
    private String color;

    @Column(name = "requires_loss_reason", nullable = false)
    private boolean requiresLossReason = false;
}
