package com.primecrm.infra.entity.commercial;

import com.primecrm.infra.entity.BaseEntity;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "leads")
public class Lead extends BaseEntity {

    @Generated(event = EventType.INSERT)
    @Column(length = 30, insertable = false, updatable = false)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "contact_name", length = 200)
    private String contactName;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String mobile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id")
    private DomainValue origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private DomainValue status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_id")
    private DomainValue priority;

    @Column(length = 150)
    private String campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id")
    private Pipeline pipeline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private PipelineStage stage;

    @Column(precision = 5, scale = 2)
    private BigDecimal probability;

    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "qualification_score")
    private Integer qualificationScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_customer_id")
    private Customer convertedCustomer;

    @Column(name = "converted_at")
    private Instant convertedAt;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
