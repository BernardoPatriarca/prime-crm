package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.contract.ContractRequest;
import com.primecrm.core.mapper.ContractMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.contract.Contract;
import com.primecrm.infra.entity.contract.ContractStatus;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.repository.ContractRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractMapper contractMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;
    @Mock
    private OrderService orderService;

    private ContractService contractService;

    @BeforeEach
    void setUp() {
        contractService = new ContractService(contractRepository, contractMapper, referenceResolver, auditService,
                orderService);
    }

    @Test
    void create_startsAsDraftAndIsAudited() {
        ContractRequest request = request();
        Contract contract = newContract(ContractStatus.TERMINATED);

        when(contractMapper.toEntity(request)).thenReturn(contract);
        when(contractRepository.save(contract)).thenReturn(contract);

        contractService.create(request);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.DRAFT);
        verify(auditService).recordCreate(contract);
    }

    @Test
    void createFromOrder_copiesCustomerAndUsesOrderTotalAsRecurringAmount() {
        UUID orderId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        Order order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setTotalAmount(new BigDecimal("899.00"));

        when(orderService.getActiveOrThrow(orderId)).thenReturn(order);
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contractService.createFromOrder(orderId);

        verify(contractRepository).save(argThatRecurringAmountEquals("899.00"));
        verify(auditService).recordCreate(any(Contract.class));
    }

    @Test
    void changeStatus_toTerminated_fillsTerminatedAt() {
        Contract contract = newContract(ContractStatus.ACTIVE);
        when(contractRepository.findByIdAndDeletedAtIsNull(contract.getId())).thenReturn(Optional.of(contract));
        when(contractRepository.save(contract)).thenReturn(contract);

        contractService.changeStatus(contract.getId(), ContractStatus.TERMINATED);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.TERMINATED);
        assertThat(contract.getTerminatedAt()).isNotNull();
        verify(auditService).recordUpdate(any(Contract.class), any());
    }

    @Test
    void changeStatus_withoutStatus_throwsBusinessException() {
        assertThatThrownBy(() -> contractService.changeStatus(UUID.randomUUID(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void delete_marksTheContractAsDeletedAndAudits() {
        Contract contract = newContract(ContractStatus.DRAFT);
        when(contractRepository.findByIdAndDeletedAtIsNull(contract.getId())).thenReturn(Optional.of(contract));

        contractService.delete(contract.getId());

        assertThat(contract.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(contract);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(contractRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contractService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Contract argThatRecurringAmountEquals(String expected) {
        return org.mockito.ArgumentMatchers.argThat(contract ->
                contract.getRecurringAmount().compareTo(new BigDecimal(expected)) == 0);
    }

    private ContractRequest request() {
        return new ContractRequest(UUID.randomUUID(), null, null, null, null, null, false, null, null);
    }

    private Contract newContract(ContractStatus status) {
        Contract contract = new Contract();
        contract.setId(UUID.randomUUID());
        contract.setStatus(status);
        return contract;
    }
}
