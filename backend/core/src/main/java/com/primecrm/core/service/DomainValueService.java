package com.primecrm.core.service;

import com.primecrm.core.audit.AuditChanges;
import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.common.ReorderItem;
import com.primecrm.core.dto.common.ReorderRequest;
import com.primecrm.core.dto.domain.DomainValueRequest;
import com.primecrm.core.dto.domain.DomainValueResponse;
import com.primecrm.core.mapper.DomainValueMapper;
import com.primecrm.core.specification.DomainValueSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.domain.DomainType;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.DomainValueRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainValueService {

    private static final String AUDIT_ENTITY = "DomainValue";

    private final DomainValueRepository domainValueRepository;
    private final DomainValueMapper domainValueMapper;
    private final DomainTypeService domainTypeService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<DomainValueResponse> list(String domainTypeCode, String search, Boolean active, Pageable pageable) {
        var spec = SpecificationUtils.<DomainValue>and(
                DomainValueSpecifications.notDeleted(),
                DomainValueSpecifications.byDomainTypeCode(domainTypeCode),
                DomainValueSpecifications.textSearch(search),
                DomainValueSpecifications.hasActive(active)
        );
        return domainValueRepository.findAll(spec, pageable).map(domainValueMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DomainValueResponse findById(UUID id) {
        return domainValueMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public DomainValueResponse create(DomainValueRequest request) {
        DomainType domainType = domainTypeService.getByCodeOrThrow(request.domainTypeCode());

        DomainValue domainValue = domainValueMapper.toEntity(request);
        domainValue.setDomainType(domainType);
        domainValue.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        domainValue.setActive(request.active() == null || request.active());

        domainValue = domainValueRepository.save(domainValue);
        auditService.recordCreate(domainValue);
        return domainValueMapper.toResponse(domainValue);
    }

    @Transactional
    public DomainValueResponse update(UUID id, DomainValueRequest request) {
        DomainValue domainValue = getActiveOrThrow(id);
        DomainType domainType = domainTypeService.getByCodeOrThrow(request.domainTypeCode());

        Map<String, Object> previousState = auditService.snapshot(domainValue);
        domainValueMapper.updateEntity(domainValue, request);
        domainValue.setDomainType(domainType);
        if (request.displayOrder() != null) {
            domainValue.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            domainValue.setActive(request.active());
        }

        domainValue = domainValueRepository.save(domainValue);
        auditService.recordUpdate(domainValue, previousState);
        return domainValueMapper.toResponse(domainValue);
    }

    @Transactional
    public void delete(UUID id) {
        DomainValue domainValue = getActiveOrThrow(id);
        domainValue.setDeletedAt(Instant.now());
        domainValueRepository.save(domainValue);
        auditService.recordDelete(domainValue);
    }

    @Transactional
    public List<DomainValueResponse> reorder(ReorderRequest request) {
        Map<UUID, Integer> desiredOrder = request.items().stream()
                .collect(Collectors.toMap(ReorderItem::id, ReorderItem::displayOrder));

        List<DomainValue> values = domainValueRepository.findAllById(desiredOrder.keySet());
        if (values.size() != desiredOrder.size()) {
            throw new ResourceNotFoundException("Um ou mais valores de dominio informados nao foram encontrados");
        }

        Map<UUID, Integer> previousOrder = values.stream()
                .collect(Collectors.toMap(DomainValue::getId, DomainValue::getDisplayOrder));

        values.forEach(value -> value.setDisplayOrder(desiredOrder.get(value.getId())));
        List<DomainValue> saved = domainValueRepository.saveAll(values);

        saved.forEach(value -> auditService.recordChange(AuditAction.UPDATE, AUDIT_ENTITY, value.getId(),
                Map.of("displayOrder", AuditChanges.of(previousOrder.get(value.getId()), value.getDisplayOrder()))));

        return saved.stream()
                .sorted(Comparator.comparingInt(DomainValue::getDisplayOrder))
                .map(domainValueMapper::toResponse)
                .toList();
    }

    private DomainValue getActiveOrThrow(UUID id) {
        return domainValueRepository.findById(id)
                .filter(v -> !v.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Valor de dominio", id));
    }
}
