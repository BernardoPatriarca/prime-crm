package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.customfield.CustomFieldRequest;
import com.primecrm.core.dto.customfield.CustomFieldResponse;
import com.primecrm.core.mapper.CustomFieldMapper;
import com.primecrm.core.specification.CustomFieldSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.config.CustomField;
import com.primecrm.infra.entity.config.FieldType;
import com.primecrm.infra.repository.CustomFieldRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomFieldService {

    private final CustomFieldRepository customFieldRepository;
    private final CustomFieldMapper customFieldMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<CustomFieldResponse> list(String targetEntity, String search, Boolean active, Pageable pageable) {
        var spec = SpecificationUtils.<CustomField>and(
                CustomFieldSpecifications.notDeleted(),
                CustomFieldSpecifications.byTargetEntity(targetEntity),
                CustomFieldSpecifications.textSearch(search),
                CustomFieldSpecifications.hasActive(active)
        );
        return customFieldRepository.findAll(spec, pageable).map(customFieldMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomFieldResponse findById(UUID id) {
        return customFieldMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public CustomFieldResponse create(CustomFieldRequest request) {
        validateOptions(request);

        CustomField customField = customFieldMapper.toEntity(request);
        customField.setRequired(request.required() != null && request.required());
        customField.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        customField.setActive(request.active() == null || request.active());

        customField = customFieldRepository.save(customField);
        auditService.recordCreate(customField);
        return customFieldMapper.toResponse(customField);
    }

    @Transactional
    public CustomFieldResponse update(UUID id, CustomFieldRequest request) {
        validateOptions(request);

        CustomField customField = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(customField);
        customFieldMapper.updateEntity(customField, request);
        if (request.required() != null) {
            customField.setRequired(request.required());
        }
        if (request.displayOrder() != null) {
            customField.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            customField.setActive(request.active());
        }

        customField = customFieldRepository.save(customField);
        auditService.recordUpdate(customField, previousState);
        return customFieldMapper.toResponse(customField);
    }

    @Transactional
    public void delete(UUID id) {
        CustomField customField = getActiveOrThrow(id);
        customField.setDeletedAt(Instant.now());
        customFieldRepository.save(customField);
        auditService.recordDelete(customField);
    }

    private void validateOptions(CustomFieldRequest request) {
        boolean needsOptions = request.fieldType() == FieldType.SELECT || request.fieldType() == FieldType.MULTISELECT;
        if (needsOptions && (request.options() == null || request.options().isEmpty())) {
            throw new BusinessException("CUSTOM_FIELD_OPTIONS_REQUIRED",
                    "Campos do tipo SELECT ou MULTISELECT exigem ao menos uma opcao em 'options'.");
        }
    }

    private CustomField getActiveOrThrow(UUID id) {
        return customFieldRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Campo personalizado", id));
    }
}
