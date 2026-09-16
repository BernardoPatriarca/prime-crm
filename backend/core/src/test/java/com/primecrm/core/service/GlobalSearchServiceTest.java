package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.search.GlobalSearchResponse;
import com.primecrm.core.dto.search.SearchResultType;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.repository.ContactRepository;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.TaskRepository;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private TaskRepository taskRepository;

    private GlobalSearchService globalSearchService;

    private final Predicate<String> allowAll = code -> true;
    private final Predicate<String> denyAll = code -> false;

    @BeforeEach
    void setUp() {
        globalSearchService = new GlobalSearchService(customerRepository, contactRepository, leadRepository,
                opportunityRepository, taskRepository);
    }

    @Test
    void search_withQueryShorterThanMinLength_returnsEmptyWithoutQuerying() {
        GlobalSearchResponse response = globalSearchService.search("a", allowAll);

        assertThat(response.total()).isZero();
        assertThat(response.results()).isEmpty();
        verify(customerRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void search_withNullQuery_returnsEmptyWithoutQuerying() {
        GlobalSearchResponse response = globalSearchService.search(null, allowAll);

        assertThat(response.total()).isZero();
        assertThat(response.results()).isEmpty();
    }

    @Test
    void search_withoutAnyPermission_returnsEmptyResults() {
        GlobalSearchResponse response = globalSearchService.search("cliente", denyAll);

        assertThat(response.total()).isZero();
        assertThat(response.results()).isEmpty();
        verify(customerRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void search_withCustomerPermission_mapsCustomerResults() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCode("CLI-000001");
        customer.setName("Acme Ltda");
        customer.setTradeName("Acme");

        when(customerRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(customer)));

        Predicate<String> onlyCustomers = code -> "CLIENTES_VIEW".equals(code);
        GlobalSearchResponse response = globalSearchService.search("acme", onlyCustomers);

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).type()).isEqualTo(SearchResultType.CUSTOMER);
        assertThat(response.results().get(0).title()).isEqualTo("Acme Ltda");
        assertThat(response.results().get(0).subtitle()).isEqualTo("Acme");
        assertThat(response.results().get(0).link()).isEqualTo("/clientes");
        verify(leadRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void search_trimsQueryBeforeEvaluatingMinLength() {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setCode("LEA-000001");
        lead.setName("Lead Teste");

        when(leadRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn((Page<Lead>) new PageImpl<>(List.of(lead)));

        Predicate<String> onlyLeads = code -> "LEADS_VIEW".equals(code);
        GlobalSearchResponse response = globalSearchService.search("  lead  ", onlyLeads);

        assertThat(response.query()).isEqualTo("lead");
        assertThat(response.total()).isEqualTo(1);
    }
}
