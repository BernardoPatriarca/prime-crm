package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.ContactResponse;
import com.primecrm.core.dto.commercial.CustomerListFilter;
import com.primecrm.core.dto.commercial.CustomerRequest;
import com.primecrm.core.dto.commercial.CustomerResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.mapper.CommercialSummaryMapper;
import com.primecrm.core.mapper.ContactMapper;
import com.primecrm.core.mapper.CustomerMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.CustomerSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.core.validation.DocumentValidator;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.CustomerTag;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.ContactRepository;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.CustomerTagRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ConflictException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String DEFAULT_COUNTRY = "Brasil";

    private final CustomerRepository customerRepository;
    private final CustomerTagRepository customerTagRepository;
    private final ContactRepository contactRepository;
    private final CustomerMapper customerMapper;
    private final ContactMapper contactMapper;
    private final CommercialSummaryMapper summaryMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<CustomerResponse> list(CustomerListFilter filter, Pageable pageable) {
        var spec = SpecificationUtils.<Customer>and(
                CustomerSpecifications.notDeleted(),
                CustomerSpecifications.withReferencesFetched(),
                CustomerSpecifications.textSearch(filter.search()),
                CustomerSpecifications.hasPersonType(filter.personType()),
                CustomerSpecifications.hasClientType(filter.clientTypeId()),
                CustomerSpecifications.hasSegment(filter.segmentId()),
                CustomerSpecifications.hasOwner(filter.ownerUserId()),
                CustomerSpecifications.hasActive(filter.active()),
                CustomerSpecifications.hasAnyTag(filter.tagIds())
        );
        Page<Customer> page = customerRepository.findAll(spec, pageable);
        Map<UUID, List<DomainValueSummaryResponse>> tagsByCustomer = loadTagsByCustomer(page.getContent());
        return page.map(customer -> customerMapper.toResponse(customer,
                tagsByCustomer.getOrDefault(customer.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(UUID id) {
        return toResponse(getActiveOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> findContacts(UUID customerId) {
        getActiveOrThrow(customerId);
        return contactRepository.findByCustomer_IdAndDeletedAtIsNullOrderByNameAsc(customerId).stream()
                .map(contactMapper::toResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String document = DocumentValidator.normalizeAndValidate(request.document());
        ensureDocumentAvailable(document, null);

        Customer customer = customerMapper.toEntity(request);
        customer.setDocument(document);
        customer.setCountry(StringUtils.hasText(request.country()) ? request.country() : DEFAULT_COUNTRY);
        customer.setActive(request.active() == null || request.active());
        applyReferences(customer, request);

        customer = customerRepository.save(customer);
        replaceTags(customer, request.tagIds());
        auditService.recordCreate(customer);
        return toResponse(customer);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = getActiveOrThrow(id);
        if (id.equals(request.parentCustomerId())) {
            throw new BusinessException("SELF_PARENT_FORBIDDEN",
                    "Um cliente nao pode ser matriz de si mesmo.");
        }

        String document = DocumentValidator.normalizeAndValidate(request.document());
        ensureDocumentAvailable(document, id);

        Map<String, Object> previousState = auditService.snapshot(customer);
        customerMapper.updateEntity(customer, request);
        customer.setDocument(document);
        if (request.active() != null) {
            customer.setActive(request.active());
        }
        applyReferences(customer, request);

        customer = customerRepository.save(customer);
        if (request.tagIds() != null) {
            replaceTags(customer, request.tagIds());
        }
        auditService.recordUpdate(customer, previousState);
        return toResponse(customer);
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = getActiveOrThrow(id);
        customer.setDeletedAt(Instant.now());
        customerRepository.save(customer);
        auditService.recordDelete(customer);
    }

    public Customer getActiveOrThrow(UUID id) {
        return customerRepository.findById(id)
                .filter(customer -> !customer.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    private void applyReferences(Customer customer, CustomerRequest request) {
        customer.setClientType(referenceResolver.domainValue(request.clientTypeId(), "Tipo de cliente"));
        customer.setSegment(referenceResolver.domainValue(request.segmentId(), "Segmento"));
        customer.setActivityBranch(referenceResolver.domainValue(request.activityBranchId(), "Ramo de atividade"));
        customer.setCategory(referenceResolver.domainValue(request.categoryId(), "Categoria"));
        customer.setOrigin(referenceResolver.domainValue(request.originId(), "Origem"));
        customer.setStatus(referenceResolver.domainValue(request.statusId(), "Status"));
        customer.setTeam(referenceResolver.domainValue(request.teamId(), "Equipe"));
        customer.setOwner(referenceResolver.user(request.ownerUserId()));
        customer.setParentCustomer(referenceResolver.customer(request.parentCustomerId()));
    }

    private void ensureDocumentAvailable(String document, UUID excludingId) {
        if (document == null) {
            return;
        }
        boolean taken = excludingId == null
                ? customerRepository.existsByDocumentAndDeletedAtIsNull(document)
                : customerRepository.existsByDocumentAndIdNotAndDeletedAtIsNull(document, excludingId);
        if (taken) {
            throw new ConflictException("Ja existe um cliente cadastrado com este documento (CPF/CNPJ)");
        }
    }

    private void replaceTags(Customer customer, List<UUID> tagIds) {
        customerTagRepository.deleteByCustomer_Id(customer.getId());
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<DomainValue> tags = referenceResolver.domainValues(tagIds, "Tags do cliente");
        List<CustomerTag> links = tags.stream().map(tag -> {
            CustomerTag link = new CustomerTag();
            link.setCustomer(customer);
            link.setDomainValue(tag);
            return link;
        }).toList();
        customerTagRepository.saveAll(links);
    }

    private CustomerResponse toResponse(Customer customer) {
        return customerMapper.toResponse(customer,
                loadTagsByCustomer(List.of(customer)).getOrDefault(customer.getId(), List.of()));
    }

    private Map<UUID, List<DomainValueSummaryResponse>> loadTagsByCustomer(List<Customer> customers) {
        if (customers.isEmpty()) {
            return Map.of();
        }
        List<UUID> customerIds = customers.stream().map(Customer::getId).toList();
        return customerTagRepository.findByCustomer_IdIn(customerIds).stream()
                .collect(Collectors.groupingBy(link -> link.getCustomer().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(link -> summaryMapper.toDomainValueSummary(link.getDomainValue()),
                                Collectors.toList())));
    }
}
