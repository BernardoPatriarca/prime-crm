package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.template.TemplateRequest;
import com.primecrm.core.dto.template.TemplateResponse;
import com.primecrm.core.mapper.TemplateMapper;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.core.specification.TemplateSpecifications;
import com.primecrm.infra.entity.config.Template;
import com.primecrm.infra.entity.config.TemplateType;
import com.primecrm.infra.repository.TemplateRepository;
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
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<TemplateResponse> list(TemplateType type, String search, Boolean active, Pageable pageable) {
        var spec = SpecificationUtils.<Template>and(
                TemplateSpecifications.notDeleted(),
                TemplateSpecifications.hasType(type),
                TemplateSpecifications.textSearch(search),
                TemplateSpecifications.hasActive(active)
        );
        return templateRepository.findAll(spec, pageable).map(templateMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TemplateResponse findById(UUID id) {
        return templateMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request) {
        validateSubject(request);

        Template template = templateMapper.toEntity(request);
        template.setActive(request.active() == null || request.active());

        template = templateRepository.save(template);
        auditService.recordCreate(template);
        return templateMapper.toResponse(template);
    }

    @Transactional
    public TemplateResponse update(UUID id, TemplateRequest request) {
        validateSubject(request);

        Template template = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(template);
        templateMapper.updateEntity(template, request);
        if (request.active() != null) {
            template.setActive(request.active());
        }

        template = templateRepository.save(template);
        auditService.recordUpdate(template, previousState);
        return templateMapper.toResponse(template);
    }

    @Transactional
    public void delete(UUID id) {
        Template template = getActiveOrThrow(id);
        template.setDeletedAt(Instant.now());
        templateRepository.save(template);
        auditService.recordDelete(template);
    }

    private void validateSubject(TemplateRequest request) {
        if (request.type() == TemplateType.EMAIL && !StringUtils.hasText(request.subject())) {
            throw new BusinessException("TEMPLATE_SUBJECT_REQUIRED",
                    "Templates do tipo EMAIL exigem o preenchimento do assunto (subject).");
        }
    }

    private Template getActiveOrThrow(UUID id) {
        return templateRepository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Template", id));
    }
}
