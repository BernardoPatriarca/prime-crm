package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.infra.repository.projection.LabeledAmountAggregate;
import com.primecrm.infra.repository.projection.StageAggregate;
import com.primecrm.infra.repository.projection.StageAmountAggregate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OpportunityRepository
        extends JpaRepository<Opportunity, UUID>, JpaSpecificationExecutor<Opportunity> {

    Optional<Opportunity> findByIdAndDeletedAtIsNull(UUID id);

    List<Opportunity> findByCustomer_IdInAndDeletedAtIsNull(Collection<UUID> customerIds);

    List<Opportunity> findByPipeline_IdAndDeletedAtIsNull(UUID pipelineId);

    long countByStage_IdAndDeletedAtIsNull(UUID stageId);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, UUID id);

    @EntityGraph(attributePaths = {"customer", "owner"})
    List<Opportunity> findByStage_IdAndOutcomeAndDeletedAtIsNullOrderByExpectedCloseDateAscOpenedAtAsc(
            UUID stageId, OpportunityOutcome outcome, Pageable pageable);

    @Query("""
            select o.stage.id as stageId,
                   count(o) as opportunityCount,
                   coalesce(sum(o.amount), 0) as totalAmount
            from Opportunity o
            where o.pipeline.id = :pipelineId
              and o.outcome = :outcome
              and o.deletedAt is null
            group by o.stage.id
            """)
    List<StageAggregate> summarizeByPipelineAndOutcome(@Param("pipelineId") UUID pipelineId,
                                                       @Param("outcome") OpportunityOutcome outcome);

    List<Opportunity> findTop10ByOwner_IdAndOutcomeAndExpectedCloseDateLessThanAndDeletedAtIsNullOrderByExpectedCloseDateAsc(
            UUID ownerId, OpportunityOutcome outcome, LocalDate limit);

    @Query("""
            select count(o) as itemCount, coalesce(sum(o.amount), 0) as totalAmount
            from Opportunity o
            where o.deletedAt is null
              and o.outcome = :outcome
              and o.openedAt >= :from and o.openedAt < :to
            """)
    AmountAggregate summarizeOpenedBetween(@Param("outcome") OpportunityOutcome outcome,
                                           @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(o) as itemCount, coalesce(sum(o.amount), 0) as totalAmount
            from Opportunity o
            where o.deletedAt is null
              and o.outcome = :outcome
              and o.closedAt >= :from and o.closedAt < :to
            """)
    AmountAggregate summarizeClosedBetween(@Param("outcome") OpportunityOutcome outcome,
                                           @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(o) as itemCount, coalesce(sum(o.amount), 0) as totalAmount
            from Opportunity o
            where o.deletedAt is null and o.outcome = :outcome
            """)
    AmountAggregate summarizeByOutcome(@Param("outcome") OpportunityOutcome outcome);

    @Query("""
            select s.name as label, s.color as color, s.displayOrder as displayOrder,
                   count(o.id) as itemCount, coalesce(sum(o.amount), 0) as totalAmount
            from PipelineStage s
            left join Opportunity o
                on o.stage = s and o.deletedAt is null and o.outcome = :outcome
            where s.pipeline.id = :pipelineId and s.deletedAt is null
            group by s.id, s.name, s.color, s.displayOrder
            order by s.displayOrder
            """)
    List<StageAmountAggregate> summarizeFunnel(@Param("pipelineId") UUID pipelineId,
                                               @Param("outcome") OpportunityOutcome outcome);

    @Query("""
            select function('to_char', o.closedAt, 'YYYY-MM') as label,
                   count(o) as itemCount, coalesce(sum(o.amount), 0) as totalAmount
            from Opportunity o
            where o.deletedAt is null and o.outcome = :outcome and o.closedAt >= :from
            group by function('to_char', o.closedAt, 'YYYY-MM')
            """)
    List<LabeledAmountAggregate> summarizeClosedByMonth(@Param("outcome") OpportunityOutcome outcome,
                                                        @Param("from") Instant from);

    @Query("""
            select function('to_char', o.openedAt, 'YYYY-MM') as label,
                   count(o) as itemCount, coalesce(sum(o.amount), 0) as totalAmount
            from Opportunity o
            where o.deletedAt is null and o.openedAt >= :from
            group by function('to_char', o.openedAt, 'YYYY-MM')
            """)
    List<LabeledAmountAggregate> summarizeOpenedByMonth(@Param("from") Instant from);

    @Query("""
            select o.owner.name as label, count(o) as itemCount, coalesce(sum(o.amount), 0) as totalAmount
            from Opportunity o
            where o.deletedAt is null and o.owner is not null
              and o.outcome = :outcome
              and o.closedAt >= :from and o.closedAt < :to
            group by o.owner.id, o.owner.name
            order by coalesce(sum(o.amount), 0) desc
            """)
    List<LabeledAmountAggregate> rankOwnersByClosedAmount(@Param("outcome") OpportunityOutcome outcome,
                                                          @Param("from") Instant from, @Param("to") Instant to,
                                                          Pageable pageable);
}
