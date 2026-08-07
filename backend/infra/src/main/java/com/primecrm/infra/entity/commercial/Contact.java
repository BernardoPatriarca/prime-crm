package com.primecrm.infra.entity.commercial;

import com.primecrm.infra.entity.BaseEntity;
import com.primecrm.infra.entity.domain.DomainValue;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contacts")
public class Contact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "position_title", length = 120)
    private String positionTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DomainValue department;

    @Column(length = 180)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String mobile;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 255)
    private String linkedin;

    @Column(name = "primary_contact", nullable = false)
    private boolean primaryContact = false;

    @Column(name = "decision_maker", nullable = false)
    private boolean decisionMaker = false;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
