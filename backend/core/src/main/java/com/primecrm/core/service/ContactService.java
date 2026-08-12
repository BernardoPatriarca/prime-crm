package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.ContactRequest;
import com.primecrm.core.dto.commercial.ContactResponse;
import com.primecrm.core.mapper.ContactMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.ContactSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.commercial.Contact;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.repository.ContactRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ContactResponse> list(UUID customerId, String search, Boolean active, Pageable pageable) {
        var spec = SpecificationUtils.<Contact>and(
                ContactSpecifications.notDeleted(),
                ContactSpecifications.withReferencesFetched(),
                ContactSpecifications.byCustomerId(customerId),
                ContactSpecifications.textSearch(search),
                ContactSpecifications.hasActive(active)
        );
        return contactRepository.findAll(spec, pageable).map(contactMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ContactResponse findById(UUID id) {
        return contactMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public ContactResponse create(ContactRequest request) {
        Customer customer = referenceResolver.customer(request.customerId());

        Contact contact = contactMapper.toEntity(request);
        contact.setCustomer(customer);
        contact.setDepartment(referenceResolver.domainValue(request.departmentId(), "Departamento"));
        contact.setPrimaryContact(Boolean.TRUE.equals(request.primaryContact()));
        contact.setDecisionMaker(Boolean.TRUE.equals(request.decisionMaker()));
        contact.setActive(request.active() == null || request.active());

        contact = contactRepository.save(contact);
        demoteOtherPrimaryContacts(contact);
        auditService.recordCreate(contact);
        return contactMapper.toResponse(contact);
    }

    @Transactional
    public ContactResponse update(UUID id, ContactRequest request) {
        Contact contact = getActiveOrThrow(id);

        Map<String, Object> previousState = auditService.snapshot(contact);
        contactMapper.updateEntity(contact, request);
        contact.setCustomer(referenceResolver.customer(request.customerId()));
        contact.setDepartment(referenceResolver.domainValue(request.departmentId(), "Departamento"));
        if (request.primaryContact() != null) {
            contact.setPrimaryContact(request.primaryContact());
        }
        if (request.decisionMaker() != null) {
            contact.setDecisionMaker(request.decisionMaker());
        }
        if (request.active() != null) {
            contact.setActive(request.active());
        }

        contact = contactRepository.save(contact);
        demoteOtherPrimaryContacts(contact);
        auditService.recordUpdate(contact, previousState);
        return contactMapper.toResponse(contact);
    }

    @Transactional
    public void delete(UUID id) {
        Contact contact = getActiveOrThrow(id);
        contact.setDeletedAt(Instant.now());
        contactRepository.save(contact);
        auditService.recordDelete(contact);
    }

    private void demoteOtherPrimaryContacts(Contact contact) {
        if (!contact.isPrimaryContact()) {
            return;
        }
        List<Contact> others = contactRepository
                .findByCustomer_IdAndPrimaryContactIsTrueAndDeletedAtIsNull(contact.getCustomer().getId()).stream()
                .filter(other -> !other.getId().equals(contact.getId()))
                .toList();
        if (others.isEmpty()) {
            return;
        }
        others.forEach(other -> other.setPrimaryContact(false));
        contactRepository.saveAll(others);
    }

    private Contact getActiveOrThrow(UUID id) {
        return contactRepository.findById(id)
                .filter(contact -> !contact.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Contato", id));
    }
}
