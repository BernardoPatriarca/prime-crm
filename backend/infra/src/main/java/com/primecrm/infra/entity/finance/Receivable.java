package com.primecrm.infra.entity.finance;

import com.primecrm.infra.entity.BaseEntity;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.contract.Contract;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.entity.order.Order;
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
@Table(name = "receivables")
public class Receivable extends BaseEntity {

    @Generated(event = EventType.INSERT)
    @Column(length = 30, insertable = false, updatable = false)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(length = 200)
    private String description;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber = 1;

    @Column(name = "total_installments", nullable = false)
    private int totalInstallments = 1;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private Instant paidAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private DomainValue paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceivableStatus status = ReceivableStatus.PENDING;

    @Column(columnDefinition = "text")
    private String notes;

    public boolean isOverdue() {
        return status == ReceivableStatus.PENDING && dueDate.isBefore(LocalDate.now());
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal remaining = amount.subtract(paidAmount);
        return remaining.signum() > 0 ? remaining : BigDecimal.ZERO;
    }
}
