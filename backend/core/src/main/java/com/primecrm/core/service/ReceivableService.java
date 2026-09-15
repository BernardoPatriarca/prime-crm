package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.finance.GenerateInstallmentsRequest;
import com.primecrm.core.dto.finance.ReceivableListFilter;
import com.primecrm.core.dto.finance.ReceivablePaymentRequest;
import com.primecrm.core.dto.finance.ReceivableRequest;
import com.primecrm.core.dto.finance.ReceivableResponse;
import com.primecrm.core.mapper.ReceivableMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.ReceivableSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.finance.Receivable;
import com.primecrm.infra.entity.finance.ReceivableStatus;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.repository.ReceivableRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final ReceivableMapper receivableMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> list(ReceivableListFilter filter, Pageable pageable) {
        return receivableRepository.findAll(toSpecification(filter), pageable).map(receivableMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ReceivableResponse findById(UUID id) {
        return receivableMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public ReceivableResponse create(ReceivableRequest request) {
        Receivable receivable = receivableMapper.toEntity(request);
        applyReferences(receivable, request);
        receivable.setStatus(ReceivableStatus.PENDING);
        receivable.setPaidAmount(BigDecimal.ZERO);

        receivable = receivableRepository.save(receivable);
        auditService.recordCreate(receivable);
        return receivableMapper.toResponse(receivable);
    }

    @Transactional
    public List<ReceivableResponse> generateFromOrder(UUID orderId, GenerateInstallmentsRequest request) {
        Order order = referenceResolver.order(orderId);
        int installments = request.installments();
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal baseInstallmentAmount = totalAmount
                .divide(BigDecimal.valueOf(installments), 2, RoundingMode.DOWN);
        BigDecimal roundingRemainder = totalAmount.subtract(baseInstallmentAmount.multiply(BigDecimal.valueOf(installments)));

        List<Receivable> created = new java.util.ArrayList<>();
        for (int i = 0; i < installments; i++) {
            Receivable receivable = new Receivable();
            receivable.setCustomer(order.getCustomer());
            receivable.setOrder(order);
            receivable.setDescription("Parcela " + (i + 1) + "/" + installments + " - Pedido " + order.getCode());
            receivable.setInstallmentNumber(i + 1);
            receivable.setTotalInstallments(installments);
            receivable.setDueDate(request.firstDueDate().plusMonths(i));
            BigDecimal installmentAmount = i == installments - 1
                    ? baseInstallmentAmount.add(roundingRemainder)
                    : baseInstallmentAmount;
            receivable.setAmount(installmentAmount);
            receivable.setStatus(ReceivableStatus.PENDING);

            receivable = receivableRepository.save(receivable);
            auditService.recordCreate(receivable);
            created.add(receivable);
        }
        return created.stream().map(receivableMapper::toResponse).toList();
    }

    @Transactional
    public ReceivableResponse update(UUID id, ReceivableRequest request) {
        Receivable receivable = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(receivable);

        receivableMapper.updateEntity(receivable, request);
        applyReferences(receivable, request);

        receivable = receivableRepository.save(receivable);
        auditService.recordUpdate(receivable, previousState);
        return receivableMapper.toResponse(receivable);
    }

    @Transactional
    public ReceivableResponse pay(UUID id, ReceivablePaymentRequest request) {
        Receivable receivable = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(receivable);

        BigDecimal amountPaid = request.amount() != null ? request.amount() : receivable.getRemainingAmount();
        receivable.setPaidAmount(receivable.getPaidAmount().add(amountPaid));
        receivable.setPaidAt(request.paidAt() != null ? request.paidAt() : Instant.now());
        if (request.paymentMethodId() != null) {
            receivable.setPaymentMethod(referenceResolver.domainValue(request.paymentMethodId(), "Forma de pagamento"));
        }
        if (receivable.getPaidAmount().compareTo(receivable.getAmount()) >= 0) {
            receivable.setStatus(ReceivableStatus.PAID);
        }

        receivable = receivableRepository.save(receivable);
        auditService.recordUpdate(receivable, previousState);
        return receivableMapper.toResponse(receivable);
    }

    @Transactional
    public void delete(UUID id) {
        Receivable receivable = getActiveOrThrow(id);
        receivable.setDeletedAt(Instant.now());
        receivableRepository.save(receivable);
        auditService.recordDelete(receivable);
    }

    private Specification<Receivable> toSpecification(ReceivableListFilter filter) {
        return SpecificationUtils.and(
                ReceivableSpecifications.notDeleted(),
                ReceivableSpecifications.withReferencesFetched(),
                ReceivableSpecifications.textSearch(filter.search()),
                ReceivableSpecifications.hasStatus(filter.status()),
                ReceivableSpecifications.hasCustomer(filter.customerId()),
                ReceivableSpecifications.hasOrder(filter.orderId()),
                ReceivableSpecifications.hasContract(filter.contractId()),
                ReceivableSpecifications.dueFrom(filter.dueFrom()),
                ReceivableSpecifications.dueTo(filter.dueTo()),
                ReceivableSpecifications.onlyOverdue(filter.overdue()));
    }

    private void applyReferences(Receivable receivable, ReceivableRequest request) {
        receivable.setCustomer(referenceResolver.customer(request.customerId()));
        receivable.setOrder(referenceResolver.order(request.orderId()));
        receivable.setContract(referenceResolver.contract(request.contractId()));
        receivable.setPaymentMethod(referenceResolver.domainValue(request.paymentMethodId(), "Forma de pagamento"));
    }

    private Receivable getActiveOrThrow(UUID id) {
        return receivableRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a receber", id));
    }
}
