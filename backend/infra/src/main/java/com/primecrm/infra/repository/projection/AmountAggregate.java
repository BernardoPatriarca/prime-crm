package com.primecrm.infra.repository.projection;

import java.math.BigDecimal;

public interface AmountAggregate {

    long getItemCount();

    BigDecimal getTotalAmount();
}
