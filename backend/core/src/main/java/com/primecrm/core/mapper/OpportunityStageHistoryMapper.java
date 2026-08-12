package com.primecrm.core.mapper;

import com.primecrm.core.dto.commercial.OpportunityStageHistoryResponse;
import com.primecrm.infra.entity.commercial.OpportunityStageHistory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = CommercialSummaryMapper.class)
public interface OpportunityStageHistoryMapper {

    OpportunityStageHistoryResponse toResponse(OpportunityStageHistory history);
}
