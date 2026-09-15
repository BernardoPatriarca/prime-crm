package com.primecrm.infra.repository.projection;

public interface LabeledCountAggregate {

    String getLabel();

    long getItemCount();
}
