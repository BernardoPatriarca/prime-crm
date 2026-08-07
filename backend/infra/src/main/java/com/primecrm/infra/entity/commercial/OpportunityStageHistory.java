package com.primecrm.infra.entity.commercial;

import com.primecrm.infra.entity.BaseEntity;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.config.PipelineStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "opportunity_stage_history")
public class OpportunityStageHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stage_id")
    private PipelineStage fromStage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_stage_id", nullable = false)
    private PipelineStage toStage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moved_by_user_id")
    private User movedByUser;

    @Column(name = "moved_at", nullable = false)
    private Instant movedAt = Instant.now();

    @Column(name = "days_in_previous_stage")
    private Integer daysInPreviousStage;

    @Column(columnDefinition = "text")
    private String note;
}
