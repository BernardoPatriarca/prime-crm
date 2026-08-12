package com.primecrm.infra.entity.commercial;

import com.primecrm.infra.entity.BaseEntity;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "opportunities")
public class Opportunity extends BaseEntity {

    @Generated(event = EventType.INSERT)
    @Column(length = 30, insertable = false, updatable = false)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private Pipeline pipeline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_id", nullable = false)
    private PipelineStage stage;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2)
    private BigDecimal probability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private DomainValue team;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OpportunityOutcome outcome = OpportunityOutcome.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "win_reason_id")
    private DomainValue winReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loss_reason_id")
    private DomainValue lossReason;

    @Column(length = 200)
    private String competitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_lead_id")
    private Lead sourceLead;

    @Column(columnDefinition = "text")
    private String notes;
}
