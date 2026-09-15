package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.finance.PayablePaymentRequest;
import com.primecrm.core.dto.finance.PayableRequest;
import com.primecrm.core.mapper.PayableMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.finance.Payable;
import com.primecrm.infra.entity.finance.PayableStatus;
import com.primecrm.infra.repository.PayableRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayableServiceTest {

    @Mock
    private PayableRepository payableRepository;
    @Mock
    private PayableMapper payableMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private PayableService payableService;

    @BeforeEach
    void setUp() {
        payableService = new PayableService(payableRepository, payableMapper, referenceResolver, auditService);
    }

    @Test
    void create_startsAsPendingWithNoPaidAmount() {
        PayableRequest request = request();
        Payable payable = newPayable(new BigDecimal("300.00"));

        when(payableMapper.toEntity(request)).thenReturn(payable);
        when(payableRepository.save(payable)).thenReturn(payable);

        payableService.create(request);

        assertThat(payable.getStatus()).isEqualTo(PayableStatus.PENDING);
        assertThat(payable.getPaidAmount()).isEqualByComparingTo("0");
        verify(auditService).recordCreate(payable);
    }

    @Test
    void pay_withoutAmount_settlesTheRemainingBalanceAndClosesIt() {
        Payable payable = newPayable(new BigDecimal("300.00"));
        when(payableRepository.findByIdAndDeletedAtIsNull(payable.getId())).thenReturn(Optional.of(payable));
        when(payableRepository.save(payable)).thenReturn(payable);

        payableService.pay(payable.getId(), new PayablePaymentRequest(null, null, null));

        assertThat(payable.getPaidAmount()).isEqualByComparingTo("300.00");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.PAID);
        assertThat(payable.getPaidAt()).isNotNull();
    }

    @Test
    void pay_withPartialAmount_keepsItPending() {
        Payable payable = newPayable(new BigDecimal("300.00"));
        when(payableRepository.findByIdAndDeletedAtIsNull(payable.getId())).thenReturn(Optional.of(payable));
        when(payableRepository.save(payable)).thenReturn(payable);

        payableService.pay(payable.getId(), new PayablePaymentRequest(new BigDecimal("100.00"), null, null));

        assertThat(payable.getPaidAmount()).isEqualByComparingTo("100.00");
        assertThat(payable.getStatus()).isEqualTo(PayableStatus.PENDING);
    }

    @Test
    void delete_marksThePayableAsDeletedAndAudits() {
        Payable payable = newPayable(new BigDecimal("50.00"));
        when(payableRepository.findByIdAndDeletedAtIsNull(payable.getId())).thenReturn(Optional.of(payable));

        payableService.delete(payable.getId());

        assertThat(payable.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(payable);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(payableRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> payableService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private PayableRequest request() {
        return new PayableRequest(UUID.randomUUID(), null, "Aluguel do escritorio", LocalDate.now(),
                new BigDecimal("300.00"), null, null);
    }

    private Payable newPayable(BigDecimal amount) {
        Payable payable = new Payable();
        payable.setId(UUID.randomUUID());
        payable.setAmount(amount);
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setStatus(PayableStatus.PENDING);
        return payable;
    }
}
