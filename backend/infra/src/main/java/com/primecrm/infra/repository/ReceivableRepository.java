package com.primecrm.infra.repository;

import com.primecrm.infra.entity.finance.Receivable;
import com.primecrm.infra.entity.finance.ReceivableStatus;
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
public interface ReceivableRepository extends JpaRepository<Receivable, UUID>, JpaSpecificationExecutor<Receivable> {

    Optional<Receivable> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select count(r) as itemCount, coalesce(sum(r.amount - r.paidAmount), 0) as totalAmount
            from Receivable r
            where r.deletedAt is null and r.status = :status
            """)
    AmountAggregate summarizeOpen(@Param("status") ReceivableStatus status);

    @Query("""
            select count(r) as itemCount, coalesce(sum(r.amount - r.paidAmount), 0) as totalAmount
            from Receivable r
            where r.deletedAt is null and r.status = :status and r.dueDate < :today
            """)
    AmountAggregate summarizeOverdue(@Param("status") ReceivableStatus status, @Param("today") LocalDate today);

    @Query("""
            select count(r) as itemCount, coalesce(sum(r.paidAmount), 0) as totalAmount
            from Receivable r
            where r.deletedAt is null and r.paidAt >= :from and r.paidAt < :to
            """)
    AmountAggregate summarizePaidBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select function('to_char', r.paidAt, 'YYYY-MM') as label,
                   count(r) as itemCount, coalesce(sum(r.paidAmount), 0) as totalAmount
            from Receivable r
            where r.deletedAt is null and r.paidAt >= :from
            group by function('to_char', r.paidAt, 'YYYY-MM')
            """)
    List<LabeledAmountAggregate> summarizePaidByMonth(@Param("from") Instant from);
}
