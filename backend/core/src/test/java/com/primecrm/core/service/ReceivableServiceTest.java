package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.finance.GenerateInstallmentsRequest;
import com.primecrm.core.dto.finance.ReceivablePaymentRequest;
import com.primecrm.core.dto.finance.ReceivableRequest;
import com.primecrm.core.mapper.ReceivableMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.finance.Receivable;
import com.primecrm.infra.entity.finance.ReceivableStatus;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.repository.ReceivableRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class ReceivableServiceTest {

    @Mock
    private ReceivableRepository receivableRepository;
    @Mock
    private ReceivableMapper receivableMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private ReceivableService receivableService;

    @BeforeEach
    void setUp() {
        receivableService = new ReceivableService(receivableRepository, receivableMapper, referenceResolver,
                auditService);
    }

    @Test
    void create_startsAsPendingWithNoPaidAmount() {
        ReceivableRequest request = request();
        Receivable receivable = newReceivable(new BigDecimal("300.00"));

        when(receivableMapper.toEntity(request)).thenReturn(receivable);
        when(receivableRepository.save(receivable)).thenReturn(receivable);

        receivableService.create(request);

        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PENDING);
        assertThat(receivable.getPaidAmount()).isEqualByComparingTo("0");
        verify(auditService).recordCreate(receivable);
    }

    @Test
    void generateFromOrder_splitsTotalIntoEqualInstallmentsWithLastAbsorbingRounding() {
        UUID orderId = UUID.randomUUID();
        Customer customer = new Customer();
        Order order = new Order();
        order.setId(orderId);
        order.setCode("PED-001000");
        order.setCustomer(customer);
        order.setTotalAmount(new BigDecimal("100.00"));

        when(referenceResolver.order(orderId)).thenReturn(order);
        when(receivableRepository.save(any(Receivable.class))).thenAnswer((Answer<Receivable>) invocation -> invocation.getArgument(0));
        when(receivableMapper.toResponse(any(Receivable.class))).thenReturn(null);

        GenerateInstallmentsRequest request = new GenerateInstallmentsRequest(3, LocalDate.of(2026, 1, 10));
        receivableService.generateFromOrder(orderId, request);

        org.mockito.ArgumentCaptor<Receivable> captor = org.mockito.ArgumentCaptor.forClass(Receivable.class);
        verify(receivableRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        List<Receivable> saved = captor.getAllValues();

        BigDecimal total = saved.stream().map(Receivable::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("100.00");
        assertThat(saved.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(saved.get(2).getDueDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(saved.get(2).getTotalInstallments()).isEqualTo(3);
    }

    @Test
    void pay_withoutAmount_settlesTheRemainingBalanceAndClosesIt() {
        Receivable receivable = newReceivable(new BigDecimal("300.00"));
        when(receivableRepository.findByIdAndDeletedAtIsNull(receivable.getId())).thenReturn(Optional.of(receivable));
        when(receivableRepository.save(receivable)).thenReturn(receivable);

        receivableService.pay(receivable.getId(), new ReceivablePaymentRequest(null, null, null));

        assertThat(receivable.getPaidAmount()).isEqualByComparingTo("300.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PAID);
        assertThat(receivable.getPaidAt()).isNotNull();
    }

    @Test
    void pay_withPartialAmount_keepsItPending() {
        Receivable receivable = newReceivable(new BigDecimal("300.00"));
        when(receivableRepository.findByIdAndDeletedAtIsNull(receivable.getId())).thenReturn(Optional.of(receivable));
        when(receivableRepository.save(receivable)).thenReturn(receivable);

        receivableService.pay(receivable.getId(), new ReceivablePaymentRequest(new BigDecimal("100.00"), null, null));

        assertThat(receivable.getPaidAmount()).isEqualByComparingTo("100.00");
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.PENDING);
    }

    @Test
    void delete_marksTheReceivableAsDeletedAndAudits() {
        Receivable receivable = newReceivable(new BigDecimal("50.00"));
        when(receivableRepository.findByIdAndDeletedAtIsNull(receivable.getId())).thenReturn(Optional.of(receivable));

        receivableService.delete(receivable.getId());

        assertThat(receivable.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(receivable);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(receivableRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> receivableService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private ReceivableRequest request() {
        return new ReceivableRequest(UUID.randomUUID(), null, null, null, 1, 1, LocalDate.now(),
                new BigDecimal("300.00"), null, null);
    }

    private Receivable newReceivable(BigDecimal amount) {
        Receivable receivable = new Receivable();
        receivable.setId(UUID.randomUUID());
        receivable.setAmount(amount);
        receivable.setPaidAmount(BigDecimal.ZERO);
        receivable.setStatus(ReceivableStatus.PENDING);
        return receivable;
    }
}
