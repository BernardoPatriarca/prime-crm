package com.primecrm.infra.repository;

import com.primecrm.infra.entity.finance.Payable;
import com.primecrm.infra.entity.finance.PayableStatus;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PayableRepository extends JpaRepository<Payable, UUID>, JpaSpecificationExecutor<Payable> {

    Optional<Payable> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select count(p) as itemCount, coalesce(sum(p.amount - p.paidAmount), 0) as totalAmount
            from Payable p
            where p.deletedAt is null and p.status = :status
            """)
    AmountAggregate summarizeOpen(@Param("status") PayableStatus status);

    @Query("""
            select count(p) as itemCount, coalesce(sum(p.amount - p.paidAmount), 0) as totalAmount
            from Payable p
            where p.deletedAt is null and p.status = :status and p.dueDate < :today
            """)
    AmountAggregate summarizeOverdue(@Param("status") PayableStatus status, @Param("today") LocalDate today);

    @Query("""
            select count(p) as itemCount, coalesce(sum(p.paidAmount), 0) as totalAmount
            from Payable p
            where p.deletedAt is null and p.paidAt >= :from and p.paidAt < :to
            """)
    AmountAggregate summarizePaidBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select function('to_char', p.paidAt, 'YYYY-MM') as label,
                   count(p) as itemCount, coalesce(sum(p.paidAmount), 0) as totalAmount
            from Payable p
            where p.deletedAt is null and p.paidAt >= :from
            group by function('to_char', p.paidAt, 'YYYY-MM')
            """)
    List<LabeledAmountAggregate> summarizePaidByMonth(@Param("from") Instant from);
}
