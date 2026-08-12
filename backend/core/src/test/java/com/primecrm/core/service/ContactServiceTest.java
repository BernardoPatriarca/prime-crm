package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.ContactRequest;
import com.primecrm.core.mapper.ContactMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.commercial.Contact;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.repository.ContactRepository;
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
class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;
    @Mock
    private ContactMapper contactMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private ContactService contactService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactRepository, contactMapper, referenceResolver, auditService);
        customer = new Customer();
        customer.setId(UUID.randomUUID());
    }

    @Test
    void create_asPrimaryContact_demotesTheOtherPrimaryContactsOfTheSameCustomer() {
        ContactRequest request = new ContactRequest(customer.getId(), "Maria Compradora", null, null, null,
                null, null, null, null, Boolean.TRUE, null, null, null);

        Contact created = newContact("Maria Compradora", true);
        Contact previousPrimary = newContact("Joao Antigo", true);

        when(referenceResolver.customer(customer.getId())).thenReturn(customer);
        when(contactMapper.toEntity(request)).thenReturn(created);
        when(contactRepository.save(created)).thenReturn(created);
        when(contactRepository.findByCustomer_IdAndPrimaryContactIsTrueAndDeletedAtIsNull(customer.getId()))
                .thenReturn(List.of(previousPrimary, created));

        contactService.create(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Contact>> captor = ArgumentCaptor.forClass(List.class);
        verify(contactRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).containsExactly(previousPrimary);
        assertThat(previousPrimary.isPrimaryContact()).isFalse();
        assertThat(created.isPrimaryContact()).isTrue();
    }

    @Test
    void create_notPrimaryContact_doesNotTouchTheOtherContacts() {
        ContactRequest request = new ContactRequest(customer.getId(), "Contato Comum", null, null, null,
                null, null, null, null, Boolean.FALSE, null, null, null);

        Contact created = newContact("Contato Comum", false);

        when(referenceResolver.customer(customer.getId())).thenReturn(customer);
        when(contactMapper.toEntity(request)).thenReturn(created);
        when(contactRepository.save(created)).thenReturn(created);

        contactService.create(request);

        verify(contactRepository, never())
                .findByCustomer_IdAndPrimaryContactIsTrueAndDeletedAtIsNull(customer.getId());
        verify(contactRepository, never()).saveAll(anyList());
    }

    @Test
    void update_promotingContactToPrimary_demotesThePreviousPrimary() {
        Contact contact = newContact("Contato Promovido", false);
        Contact previousPrimary = newContact("Contato Anterior", true);

        ContactRequest request = new ContactRequest(customer.getId(), "Contato Promovido", null, null, null,
                null, null, null, null, Boolean.TRUE, null, null, null);

        when(contactRepository.findById(contact.getId())).thenReturn(Optional.of(contact));
        when(referenceResolver.customer(customer.getId())).thenReturn(customer);
        when(contactRepository.save(contact)).thenReturn(contact);
        when(contactRepository.findByCustomer_IdAndPrimaryContactIsTrueAndDeletedAtIsNull(customer.getId()))
                .thenReturn(List.of(previousPrimary, contact));

        contactService.update(contact.getId(), request);

        assertThat(contact.isPrimaryContact()).isTrue();
        assertThat(previousPrimary.isPrimaryContact()).isFalse();
        verify(contactRepository).saveAll(List.of(previousPrimary));
    }

    private Contact newContact(String name, boolean primary) {
        Contact contact = new Contact();
        contact.setId(UUID.randomUUID());
        contact.setCustomer(customer);
        contact.setName(name);
        contact.setPrimaryContact(primary);
        return contact;
    }
}
