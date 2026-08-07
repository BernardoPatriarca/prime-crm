package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.Opportunity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
