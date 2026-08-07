package com.primecrm.infra.entity.commercial;

import com.primecrm.infra.entity.BaseEntity;
import com.primecrm.infra.entity.auth.User;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Column(length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "trade_name", length = 200)
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "person_type", nullable = false, length = 10)
    private PersonType personType = PersonType.JURIDICA;

    @Column(length = 20)
    private String document;

    @Column(name = "state_registration", length = 30)
    private String stateRegistration;

    @Column(name = "municipal_registration", length = 30)
    private String municipalRegistration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_type_id")
    private DomainValue clientType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id")
    private DomainValue segment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_branch_id")
    private DomainValue activityBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private DomainValue category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id")
    private DomainValue origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private DomainValue status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private DomainValue team;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String mobile;

    @Column(length = 180)
    private String email;

    @Column(name = "financial_email", length = 180)
    private String financialEmail;

    @Column(length = 255)
    private String website;

    @Column(length = 120)
    private String instagram;

    @Column(length = 255)
    private String linkedin;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(length = 200)
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(length = 120)
    private String complement;

    @Column(length = 120)
    private String district;

    @Column(length = 120)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(nullable = false, length = 60)
    private String country = "Brasil";

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "last_contact_at")
    private Instant lastContactAt;

    @Column(name = "next_contact_at")
    private Instant nextContactAt;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "payment_terms", length = 120)
    private String paymentTerms;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_customer_id")
    private Customer parentCustomer;

    @Column(nullable = false)
    private boolean active = true;
}
