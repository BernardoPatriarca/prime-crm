package com.primecrm.infra.repository;

import com.primecrm.infra.entity.proposal.Proposal;
import com.primecrm.infra.entity.proposal.ProposalStatus;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
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
public interface ProposalRepository extends JpaRepository<Proposal, UUID>, JpaSpecificationExecutor<Proposal> {

    Optional<Proposal> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select count(p) as itemCount, coalesce(sum(p.totalAmount), 0) as totalAmount
            from Proposal p
            where p.deletedAt is null and p.issueDate >= :from and p.issueDate < :to
            """)
    AmountAggregate summarizeIssuedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select count(p) as itemCount, coalesce(sum(p.totalAmount), 0) as totalAmount
            from Proposal p
            where p.deletedAt is null and p.status = :status and p.issueDate >= :from and p.issueDate < :to
            """)
    AmountAggregate summarizeByStatusIssuedBetween(@Param("status") ProposalStatus status,
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select function('to_char', p.issueDate, 'YYYY-MM') as label,
                   count(p) as itemCount, coalesce(sum(p.totalAmount), 0) as totalAmount
            from Proposal p
            where p.deletedAt is null and p.issueDate >= :from
            group by function('to_char', p.issueDate, 'YYYY-MM')
            """)
    List<LabeledAmountAggregate> summarizeIssuedByMonth(@Param("from") LocalDate from);
}
