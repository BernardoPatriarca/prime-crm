package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.finance.PayableListFilter;
import com.primecrm.core.dto.finance.PayablePaymentRequest;
import com.primecrm.core.dto.finance.PayableRequest;
import com.primecrm.core.dto.finance.PayableResponse;
import com.primecrm.core.mapper.PayableMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.PayableSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.finance.Payable;
import com.primecrm.infra.entity.finance.PayableStatus;
import com.primecrm.infra.repository.PayableRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
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
public class PayableService {

    private final PayableRepository payableRepository;
    private final PayableMapper payableMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<PayableResponse> list(PayableListFilter filter, Pageable pageable) {
        return payableRepository.findAll(toSpecification(filter), pageable).map(payableMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PayableResponse findById(UUID id) {
        return payableMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public PayableResponse create(PayableRequest request) {
        Payable payable = payableMapper.toEntity(request);
        applyReferences(payable, request);
        payable.setStatus(PayableStatus.PENDING);
        payable.setPaidAmount(BigDecimal.ZERO);

        payable = payableRepository.save(payable);
        auditService.recordCreate(payable);
        return payableMapper.toResponse(payable);
    }

    @Transactional
    public PayableResponse update(UUID id, PayableRequest request) {
        Payable payable = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(payable);

        payableMapper.updateEntity(payable, request);
        applyReferences(payable, request);

        payable = payableRepository.save(payable);
        auditService.recordUpdate(payable, previousState);
        return payableMapper.toResponse(payable);
    }

    @Transactional
    public PayableResponse pay(UUID id, PayablePaymentRequest request) {
        Payable payable = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(payable);

        BigDecimal amountPaid = request.amount() != null ? request.amount() : payable.getRemainingAmount();
        payable.setPaidAmount(payable.getPaidAmount().add(amountPaid));
        payable.setPaidAt(request.paidAt() != null ? request.paidAt() : Instant.now());
        if (request.paymentMethodId() != null) {
            payable.setPaymentMethod(referenceResolver.domainValue(request.paymentMethodId(), "Forma de pagamento"));
        }
        if (payable.getPaidAmount().compareTo(payable.getAmount()) >= 0) {
            payable.setStatus(PayableStatus.PAID);
        }

        payable = payableRepository.save(payable);
        auditService.recordUpdate(payable, previousState);
        return payableMapper.toResponse(payable);
    }

    @Transactional
    public void delete(UUID id) {
        Payable payable = getActiveOrThrow(id);
        payable.setDeletedAt(Instant.now());
        payableRepository.save(payable);
        auditService.recordDelete(payable);
    }

    private Specification<Payable> toSpecification(PayableListFilter filter) {
        return SpecificationUtils.and(
                PayableSpecifications.notDeleted(),
                PayableSpecifications.withReferencesFetched(),
                PayableSpecifications.textSearch(filter.search()),
                PayableSpecifications.hasStatus(filter.status()),
                PayableSpecifications.hasSupplier(filter.supplierId()),
                PayableSpecifications.hasCategory(filter.categoryId()),
                PayableSpecifications.dueFrom(filter.dueFrom()),
                PayableSpecifications.dueTo(filter.dueTo()),
                PayableSpecifications.onlyOverdue(filter.overdue()));
    }

    private void applyReferences(Payable payable, PayableRequest request) {
        payable.setSupplier(referenceResolver.customer(request.supplierId()));
        payable.setCategory(referenceResolver.domainValue(request.categoryId(), "Categoria"));
        payable.setPaymentMethod(referenceResolver.domainValue(request.paymentMethodId(), "Forma de pagamento"));
    }

    private Payable getActiveOrThrow(UUID id) {
        return payableRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a pagar", id));
    }
}
