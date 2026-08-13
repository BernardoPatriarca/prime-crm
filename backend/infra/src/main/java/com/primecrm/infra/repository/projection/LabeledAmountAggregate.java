package com.primecrm.infra.repository.projection;

public interface LabeledAmountAggregate extends AmountAggregate {

    String getLabel();
}
