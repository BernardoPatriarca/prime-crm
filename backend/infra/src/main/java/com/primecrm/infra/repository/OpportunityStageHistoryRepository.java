package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.OpportunityStageHistory;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OpportunityStageHistoryRepository
        extends JpaRepository<OpportunityStageHistory, UUID>, JpaSpecificationExecutor<OpportunityStageHistory> {

    @EntityGraph(attributePaths = {"opportunity", "fromStage", "toStage", "movedByUser"})
    List<OpportunityStageHistory> findByOpportunity_IdInAndDeletedAtIsNullOrderByMovedAtAsc(
            Collection<UUID> opportunityIds);

    @EntityGraph(attributePaths = {"fromStage", "toStage", "movedByUser"})
    List<OpportunityStageHistory> findByOpportunity_IdAndDeletedAtIsNullOrderByMovedAtAsc(UUID opportunityId);

    OpportunityStageHistory findFirstByOpportunity_IdAndDeletedAtIsNullOrderByMovedAtDesc(UUID opportunityId);
}
