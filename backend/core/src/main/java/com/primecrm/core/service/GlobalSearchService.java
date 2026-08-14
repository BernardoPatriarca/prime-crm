package com.primecrm.core.service;

import com.primecrm.core.dto.search.GlobalSearchResponse;
import com.primecrm.core.dto.search.SearchResultResponse;
import com.primecrm.core.dto.search.SearchResultType;
import com.primecrm.core.specification.ContactSpecifications;
import com.primecrm.core.specification.CustomerSpecifications;
import com.primecrm.core.specification.LeadSpecifications;
import com.primecrm.core.specification.OpportunitySpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.core.specification.TaskSpecifications;
import com.primecrm.infra.entity.commercial.Contact;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.task.Task;
import com.primecrm.infra.repository.ContactRepository;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.TaskRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GlobalSearchService {

    public static final int RESULTS_PER_TYPE = 5;
    public static final int MIN_QUERY_LENGTH = 2;

    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(String query, Predicate<String> hasPermission) {
        String term = query == null ? "" : query.trim();
        if (term.length() < MIN_QUERY_LENGTH) {
            return new GlobalSearchResponse(term, 0, List.of());
        }

        PageRequest limit = PageRequest.of(0, RESULTS_PER_TYPE);
        List<SearchResultResponse> results = new ArrayList<>();

        if (hasPermission.test("CLIENTES_VIEW")) {
            customerRepository
                    .findAll(SpecificationUtils.and(CustomerSpecifications.notDeleted(),
                            CustomerSpecifications.textSearch(term)), limit)
                    .forEach(customer -> results.add(toResult(customer)));
        }
        if (hasPermission.test("CONTATOS_VIEW")) {
            contactRepository
                    .findAll(SpecificationUtils.and(ContactSpecifications.notDeleted(),
                            ContactSpecifications.textSearch(term)), limit)
                    .forEach(contact -> results.add(toResult(contact)));
        }
        if (hasPermission.test("LEADS_VIEW")) {
            leadRepository
                    .findAll(SpecificationUtils.and(LeadSpecifications.notDeleted(),
                            LeadSpecifications.textSearch(term)), limit)
                    .forEach(lead -> results.add(toResult(lead)));
        }
        if (hasPermission.test("OPORTUNIDADES_VIEW")) {
            opportunityRepository
                    .findAll(SpecificationUtils.and(OpportunitySpecifications.notDeleted(),
                            OpportunitySpecifications.textSearch(term)), limit)
                    .forEach(opportunity -> results.add(toResult(opportunity)));
        }
        if (hasPermission.test("TAREFAS_VIEW")) {
            taskRepository
                    .findAll(SpecificationUtils.and(TaskSpecifications.notDeleted(),
                            TaskSpecifications.textSearch(term)), limit)
                    .forEach(task -> results.add(toResult(task)));
        }

        return new GlobalSearchResponse(term, results.size(), results);
    }

    private SearchResultResponse toResult(Customer customer) {
        return new SearchResultResponse(SearchResultType.CUSTOMER, customer.getId(), customer.getCode(),
                customer.getName(), customer.getTradeName(), "/clientes");
    }

    private SearchResultResponse toResult(Contact contact) {
        return new SearchResultResponse(SearchResultType.CONTACT, contact.getId(), null, contact.getName(),
                StringUtils.hasText(contact.getEmail()) ? contact.getEmail() : contact.getPositionTitle(),
                "/contatos");
    }

    private SearchResultResponse toResult(Lead lead) {
        return new SearchResultResponse(SearchResultType.LEAD, lead.getId(), lead.getCode(), lead.getName(),
                lead.getCompanyName(), "/leads");
    }

    private SearchResultResponse toResult(Opportunity opportunity) {
        return new SearchResultResponse(SearchResultType.OPPORTUNITY, opportunity.getId(), opportunity.getCode(),
                opportunity.getTitle(), opportunity.getCustomer() == null ? null : opportunity.getCustomer().getName(),
                "/oportunidades");
    }

    private SearchResultResponse toResult(Task task) {
        return new SearchResultResponse(SearchResultType.TASK, task.getId(), task.getCode(), task.getTitle(),
                task.getCustomer() == null ? null : task.getCustomer().getName(), "/tarefas");
    }
}
