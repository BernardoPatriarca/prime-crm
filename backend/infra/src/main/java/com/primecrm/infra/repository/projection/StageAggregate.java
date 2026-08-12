package com.primecrm.infra.repository.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface StageAggregate {

    UUID getStageId();

    long getOpportunityCount();

    BigDecimal getTotalAmount();
}
