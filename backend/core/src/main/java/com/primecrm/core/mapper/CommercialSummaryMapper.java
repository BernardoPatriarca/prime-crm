package com.primecrm.core.mapper;

import com.primecrm.core.dto.common.ContactSummaryResponse;
import com.primecrm.core.dto.common.CustomerSummaryResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.dto.common.LeadSummaryResponse;
import com.primecrm.core.dto.common.OpportunitySummaryResponse;
import com.primecrm.core.dto.common.PipelineStageSummaryResponse;
import com.primecrm.core.dto.common.PipelineSummaryResponse;
import com.primecrm.core.dto.common.UserSummaryResponse;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.commercial.Contact;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainValue;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommercialSummaryMapper {

    DomainValueSummaryResponse toDomainValueSummary(DomainValue domainValue);

    List<DomainValueSummaryResponse> toDomainValueSummaryList(List<DomainValue> domainValues);

    UserSummaryResponse toUserSummary(User user);

    CustomerSummaryResponse toCustomerSummary(Customer customer);

    ContactSummaryResponse toContactSummary(Contact contact);

    LeadSummaryResponse toLeadSummary(Lead lead);

    OpportunitySummaryResponse toOpportunitySummary(Opportunity opportunity);

    PipelineSummaryResponse toPipelineSummary(Pipeline pipeline);

    PipelineStageSummaryResponse toPipelineStageSummary(PipelineStage stage);
}
