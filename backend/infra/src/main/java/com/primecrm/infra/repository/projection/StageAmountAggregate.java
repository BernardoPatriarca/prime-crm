package com.primecrm.infra.repository.projection;

public interface StageAmountAggregate extends LabeledAmountAggregate {

    String getColor();

    int getDisplayOrder();
}
