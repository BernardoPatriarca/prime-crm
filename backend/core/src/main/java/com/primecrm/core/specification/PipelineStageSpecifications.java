package com.primecrm.core.specification;

import com.primecrm.infra.entity.config.PipelineStage;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class PipelineStageSpecifications {

    private PipelineStageSpecifications() {
    }

    public static Specification<PipelineStage> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<PipelineStage> byPipelineId(UUID pipelineId) {
        if (pipelineId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("pipeline").get("id"), pipelineId);
    }
}
