package com.primecrm.core.dto.dashboard;

import java.util.List;
import java.util.UUID;

public record DashboardFunnel(
        UUID pipelineId,
        String pipelineName,
        List<DashboardFunnelStage> stages
) {
}
