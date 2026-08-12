package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.CustomerRequest;
import com.primecrm.core.dto.commercial.CustomerResponse;
import com.primecrm.core.dto.commercial.LeadConvertRequest;
import com.primecrm.core.dto.commercial.LeadConvertResponse;
import com.primecrm.core.dto.commercial.LeadResponse;
import com.primecrm.core.dto.commercial.OpportunityRequest;
import com.primecrm.core.dto.commercial.OpportunityResponse;
import com.primecrm.core.mapper.CommercialSummaryMapper;
import com.primecrm.core.mapper.LeadMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.PersonType;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.LeadTagRepository;
import com.primecrm.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private LeadTagRepository leadTagRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LeadMapper leadMapper;
    @Mock
    private CommercialSummaryMapper summaryMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private CustomerService customerService;
    @Mock
    private OpportunityService opportunityService;
    @Mock
    private AuditService auditService;

    private LeadService leadService;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(leadRepository, leadTagRepository, customerRepository, leadMapper,
                summaryMapper, referenceResolver, customerService, opportunityService, auditService);
    }

    @Test
    void convert_leadAlreadyConverted_throwsBusinessException() {
        Lead lead = newLead();
        lead.setConvertedAt(Instant.now());

        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));

        assertThatThrownBy(() -> leadService.convert(lead.getId(), emptyConvertRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ja foi convertido");

        verifyNoInteractions(customerService, opportunityService);
    }

    @Test
    void convert_withCompanyName_createsJuridicaCustomerAndStampsTheLead() {
        Lead lead = newLead();
        lead.setCompanyName("Prospect Industria Ltda");
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);

        stubTagLookup(lead);
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(customerService.create(any(CustomerRequest.class)))
                .thenReturn(CustomerResponse.builder().id(customerId).build());
        when(customerRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));
        when(leadRepository.save(lead)).thenReturn(lead);
        when(leadMapper.toResponse(eq(lead), anyList())).thenReturn(LeadResponse.builder().build());

        LeadConvertResponse response = leadService.convert(lead.getId(), emptyConvertRequest());

        ArgumentCaptor<CustomerRequest> captor = ArgumentCaptor.forClass(CustomerRequest.class);
        verify(customerService).create(captor.capture());
        CustomerRequest created = captor.getValue();

        assertThat(created.name()).isEqualTo("Prospect Industria Ltda");
        assertThat(created.tradeName()).isEqualTo(lead.getName());
        assertThat(created.personType()).isEqualTo(PersonType.JURIDICA);
        assertThat(created.email()).isEqualTo(lead.getEmail());

        assertThat(lead.getConvertedCustomer()).isSameAs(customer);
        assertThat(lead.getConvertedAt()).isNotNull();
        assertThat(response.opportunity()).isNull();
        verifyNoInteractions(opportunityService);
    }

    @Test
    void convert_withoutCompanyName_createsFisicaCustomer() {
        Lead lead = newLead();
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);

        stubTagLookup(lead);
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(customerService.create(any(CustomerRequest.class)))
                .thenReturn(CustomerResponse.builder().id(customerId).build());
        when(customerRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));
        when(leadRepository.save(lead)).thenReturn(lead);
        when(leadMapper.toResponse(eq(lead), anyList())).thenReturn(LeadResponse.builder().build());

        leadService.convert(lead.getId(), emptyConvertRequest());

        ArgumentCaptor<CustomerRequest> captor = ArgumentCaptor.forClass(CustomerRequest.class);
        verify(customerService).create(captor.capture());

        assertThat(captor.getValue().personType()).isEqualTo(PersonType.FISICA);
        assertThat(captor.getValue().name()).isEqualTo(lead.getName());
        assertThat(captor.getValue().tradeName()).isNull();
    }

    @Test
    void convert_requestingOpportunity_createsItLinkedToTheLeadAndTheNewCustomer() {
        Lead lead = newLead();
        UUID pipelineId = UUID.randomUUID();
        Pipeline pipeline = new Pipeline();
        pipeline.setId(pipelineId);
        lead.setPipeline(pipeline);
        lead.setEstimatedValue(new BigDecimal("9000.00"));

        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);

        LeadConvertRequest request = new LeadConvertRequest(null, null, null, Boolean.TRUE, null, null,
                null, null, null);

        stubTagLookup(lead);
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(customerService.create(any(CustomerRequest.class)))
                .thenReturn(CustomerResponse.builder().id(customerId).build());
        when(customerRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));
        when(leadRepository.save(lead)).thenReturn(lead);
        when(leadMapper.toResponse(eq(lead), anyList())).thenReturn(LeadResponse.builder().build());
        when(opportunityService.create(any(OpportunityRequest.class)))
                .thenReturn(OpportunityResponse.builder().build());

        LeadConvertResponse response = leadService.convert(lead.getId(), request);

        ArgumentCaptor<OpportunityRequest> captor = ArgumentCaptor.forClass(OpportunityRequest.class);
        verify(opportunityService).create(captor.capture());
        OpportunityRequest created = captor.getValue();

        assertThat(created.customerId()).isEqualTo(customerId);
        assertThat(created.pipelineId()).isEqualTo(pipelineId);
        assertThat(created.sourceLeadId()).isEqualTo(lead.getId());
        assertThat(created.title()).isEqualTo(lead.getName());
        assertThat(created.amount()).isEqualByComparingTo("9000.00");
        assertThat(response.opportunity()).isNotNull();
    }

    @Test
    void convert_requestingOpportunityWithoutAnyPipeline_throwsBusinessException() {
        Lead lead = newLead();
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);

        LeadConvertRequest request = new LeadConvertRequest(null, null, null, Boolean.TRUE, null, null,
                null, null, null);

        stubTagLookup(lead);
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(customerService.create(any(CustomerRequest.class)))
                .thenReturn(CustomerResponse.builder().id(customerId).build());
        when(customerRepository.findByIdAndDeletedAtIsNull(customerId)).thenReturn(Optional.of(customer));
        when(leadRepository.save(lead)).thenReturn(lead);

        assertThatThrownBy(() -> leadService.convert(lead.getId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("funil");

        verify(opportunityService, never()).create(any(OpportunityRequest.class));
    }

    private void stubTagLookup(Lead lead) {
        when(leadTagRepository.findByLead_IdIn(anyCollection())).thenReturn(List.of());
    }

    private LeadConvertRequest emptyConvertRequest() {
        return new LeadConvertRequest(null, null, null, null, null, null, null, null, null);
    }

    private Lead newLead() {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setName("Contato Prospect");
        lead.setEmail("prospect@lead.local");
        lead.setPhone("1130001000");
        return lead;
    }
}
