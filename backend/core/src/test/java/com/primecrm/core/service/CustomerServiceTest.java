package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.CustomerRequest;
import com.primecrm.core.mapper.CommercialSummaryMapper;
import com.primecrm.core.mapper.ContactMapper;
import com.primecrm.core.mapper.CustomerMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.PersonType;
import com.primecrm.infra.repository.ContactRepository;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.CustomerTagRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ConflictException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String MASKED_CPF = "529.982.247-25";
    private static final String CPF_DIGITS = "52998224725";

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerTagRepository customerTagRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private ContactMapper contactMapper;
    @Mock
    private CommercialSummaryMapper summaryMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, customerTagRepository, contactRepository,
                customerMapper, contactMapper, summaryMapper, referenceResolver, auditService);
    }

    @Test
    void create_withDocumentAlreadyUsed_throwsConflictAndDoesNotSave() {
        CustomerRequest request = CustomerRequest.builder()
                .name("Cliente Duplicado")
                .personType(PersonType.FISICA)
                .document(MASKED_CPF)
                .build();

        when(customerRepository.existsByDocumentAndDeletedAtIsNull(CPF_DIGITS)).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(ConflictException.class);

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void create_maskedDocument_isStoredAsDigitsOnlyAndCountryDefaultsToBrasil() {
        CustomerRequest request = CustomerRequest.builder()
                .name("Cliente Novo")
                .personType(PersonType.FISICA)
                .document(MASKED_CPF)
                .build();

        Customer entity = new Customer();
        entity.setId(UUID.randomUUID());
        entity.setCountry(null);

        when(customerRepository.existsByDocumentAndDeletedAtIsNull(CPF_DIGITS)).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(entity);
        when(customerRepository.save(entity)).thenReturn(entity);
        when(customerTagRepository.findByCustomer_IdIn(anyCollection())).thenReturn(List.of());

        customerService.create(request);

        assertThat(entity.getDocument()).isEqualTo(CPF_DIGITS);
        assertThat(entity.getCountry()).isEqualTo("Brasil");
        assertThat(entity.isActive()).isTrue();
        verify(auditService).recordCreate(entity);
    }

    @Test
    void create_withInvalidDocument_throwsBusinessException() {
        CustomerRequest request = CustomerRequest.builder()
                .name("Cliente Invalido")
                .personType(PersonType.FISICA)
                .document("111.111.111-11")
                .build();

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessException.class);

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_withItselfAsParentCustomer_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer();
        existing.setId(id);

        CustomerRequest request = CustomerRequest.builder()
                .name("Cliente")
                .personType(PersonType.JURIDICA)
                .parentCustomerId(id)
                .build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> customerService.update(id, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("matriz de si mesmo");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_withDocumentOwnedByAnotherCustomer_throwsConflict() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer();
        existing.setId(id);

        CustomerRequest request = CustomerRequest.builder()
                .name("Cliente")
                .personType(PersonType.FISICA)
                .document(MASKED_CPF)
                .build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByDocumentAndIdNotAndDeletedAtIsNull(CPF_DIGITS, id)).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(id, request))
                .isInstanceOf(ConflictException.class);

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void update_withoutTagIds_keepsExistingTagsUntouched() {
        UUID id = UUID.randomUUID();
        Customer existing = new Customer();
        existing.setId(id);

        CustomerRequest request = CustomerRequest.builder()
                .name("Cliente Sem Tags No Payload")
                .personType(PersonType.JURIDICA)
                .build();

        when(customerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(customerRepository.save(existing)).thenReturn(existing);
        when(customerTagRepository.findByCustomer_IdIn(anyCollection())).thenReturn(List.of());
        when(customerMapper.toResponse(eq(existing), anyList())).thenReturn(null);

        customerService.update(id, request);

        verify(customerTagRepository, never()).deleteByCustomer_Id(id);
        verify(auditService).recordUpdate(eq(existing), any());
    }
}
