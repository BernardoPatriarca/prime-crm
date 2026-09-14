package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.contract.ContractListFilter;
import com.primecrm.core.dto.contract.ContractRequest;
import com.primecrm.core.dto.contract.ContractResponse;
import com.primecrm.core.mapper.ContractMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.ContractSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.contract.Contract;
import com.primecrm.infra.entity.contract.ContractStatus;
import com.primecrm.infra.entity.order.Order;
import com.primecrm.infra.repository.ContractRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
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
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public Page<ContractResponse> list(ContractListFilter filter, Pageable pageable) {
        return contractRepository.findAll(toSpecification(filter), pageable).map(contractMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ContractResponse findById(UUID id) {
        return contractMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public ContractResponse create(ContractRequest request) {
        Contract contract = contractMapper.toEntity(request);
        applyReferences(contract, request);
        contract.setStatus(ContractStatus.DRAFT);
        if (contract.getStartDate() == null) {
            contract.setStartDate(LocalDate.now());
        }

        contract = contractRepository.save(contract);
        auditService.recordCreate(contract);
        return contractMapper.toResponse(contract);
    }

    @Transactional
    public ContractResponse createFromOrder(UUID orderId) {
        Order order = orderService.getActiveOrThrow(orderId);

        Contract contract = new Contract();
        contract.setCustomer(order.getCustomer());
        contract.setOrder(order);
        contract.setOpportunity(order.getOpportunity());
        contract.setOwner(order.getOwner());
        contract.setStatus(ContractStatus.DRAFT);
        contract.setStartDate(LocalDate.now());
        contract.setRecurringAmount(order.getTotalAmount());

        contract = contractRepository.save(contract);
        auditService.recordCreate(contract);
        return contractMapper.toResponse(contract);
    }

    @Transactional
    public ContractResponse update(UUID id, ContractRequest request) {
        Contract contract = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(contract);

        contractMapper.updateEntity(contract, request);
        applyReferences(contract, request);

        contract = contractRepository.save(contract);
        auditService.recordUpdate(contract, previousState);
        return contractMapper.toResponse(contract);
    }

    @Transactional
    public ContractResponse changeStatus(UUID id, ContractStatus status) {
        if (status == null) {
            throw new BusinessException("CONTRACT_STATUS_REQUIRED", "Informe o novo status do contrato");
        }
        Contract contract = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(contract);

        contract.setStatus(status);
        contract.setTerminatedAt(status.isClosed() ? Instant.now() : null);

        contract = contractRepository.save(contract);
        auditService.recordUpdate(contract, previousState);
        return contractMapper.toResponse(contract);
    }

    @Transactional
    public void delete(UUID id) {
        Contract contract = getActiveOrThrow(id);
        contract.setDeletedAt(Instant.now());
        contractRepository.save(contract);
        auditService.recordDelete(contract);
    }

    private Specification<Contract> toSpecification(ContractListFilter filter) {
        return SpecificationUtils.and(
                ContractSpecifications.notDeleted(),
                ContractSpecifications.withReferencesFetched(),
                ContractSpecifications.textSearch(filter.search()),
                ContractSpecifications.hasStatus(filter.status()),
                ContractSpecifications.hasCustomer(filter.customerId()),
                ContractSpecifications.hasOpportunity(filter.opportunityId()),
                ContractSpecifications.hasOwner(filter.ownerUserId()),
                ContractSpecifications.onlyExpired(filter.expired()));
    }

    private void applyReferences(Contract contract, ContractRequest request) {
        contract.setCustomer(referenceResolver.customer(request.customerId()));
        contract.setOpportunity(referenceResolver.opportunity(request.opportunityId()));
        contract.setOwner(referenceResolver.user(request.ownerUserId()));
        contract.setBillingCycle(referenceResolver.domainValue(request.billingCycleId(), "Ciclo de faturamento"));
    }

    private Contract getActiveOrThrow(UUID id) {
        return contractRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contrato", id));
    }
}
