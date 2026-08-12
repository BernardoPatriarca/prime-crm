package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.repository.projection.StageAggregate;
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
}
