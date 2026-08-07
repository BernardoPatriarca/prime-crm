package com.primecrm.infra.repository;

import com.primecrm.infra.entity.config.PipelineStage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PipelineStageRepository
        extends JpaRepository<PipelineStage, UUID>, JpaSpecificationExecutor<PipelineStage> {

    List<PipelineStage> findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(Collection<UUID> pipelineIds);
}
