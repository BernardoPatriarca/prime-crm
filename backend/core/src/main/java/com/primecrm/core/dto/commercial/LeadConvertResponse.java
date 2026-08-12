package com.primecrm.core.dto.commercial;

public record LeadConvertResponse(
        LeadResponse lead,
        CustomerResponse customer,
        OpportunityResponse opportunity
) {
}
