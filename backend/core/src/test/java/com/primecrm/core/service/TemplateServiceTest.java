package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.template.TemplateRequest;
import com.primecrm.core.dto.template.TemplateResponse;
import com.primecrm.core.mapper.TemplateMapper;
import com.primecrm.infra.entity.config.Template;
import com.primecrm.infra.entity.config.TemplateType;
import com.primecrm.infra.repository.TemplateRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateMapper templateMapper;
    @Mock
    private AuditService auditService;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateRepository, templateMapper, auditService);
    }

    @Test
    void create_emailWithoutSubject_throwsBusinessException() {
        TemplateRequest request = new TemplateRequest(TemplateType.EMAIL, "Boas-vindas", null, "Ola {{nome}}", null);

        assertThatThrownBy(() -> templateService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("EMAIL");

        verify(templateRepository, never()).save(any(Template.class));
    }

    @Test
    void create_emailWithBlankSubject_throwsBusinessException() {
        TemplateRequest request = new TemplateRequest(TemplateType.EMAIL, "Boas-vindas", "   ", "Ola", null);

        assertThatThrownBy(() -> templateService.create(request))
                .isInstanceOf(BusinessException.class);

        verify(templateRepository, never()).save(any(Template.class));
    }

    @Test
    void create_nonEmailWithoutSubject_savesTemplate() {
        TemplateRequest request = new TemplateRequest(TemplateType.WHATSAPP, "Follow-up", null, "Ola", null);

        Template mapped = new Template();
        Template saved = new Template();
        saved.setId(UUID.randomUUID());
        TemplateResponse expected = new TemplateResponse(saved.getId(), TemplateType.WHATSAPP, "Follow-up", null,
                "Ola", true);

        when(templateMapper.toEntity(request)).thenReturn(mapped);
        when(templateRepository.save(mapped)).thenReturn(saved);
        when(templateMapper.toResponse(saved)).thenReturn(expected);

        assertThat(templateService.create(request)).isEqualTo(expected);
        assertThat(mapped.isActive()).isTrue();
        verify(auditService).recordCreate(saved);
    }

    @Test
    void create_emailWithSubject_savesTemplateHonouringInactiveFlag() {
        TemplateRequest request = new TemplateRequest(TemplateType.EMAIL, "Boas-vindas", "Bem-vindo!", "Ola", false);

        Template mapped = new Template();
        Template saved = new Template();
        saved.setId(UUID.randomUUID());
        TemplateResponse expected = new TemplateResponse(saved.getId(), TemplateType.EMAIL, "Boas-vindas",
                "Bem-vindo!", "Ola", false);

        when(templateMapper.toEntity(request)).thenReturn(mapped);
        when(templateRepository.save(mapped)).thenReturn(saved);
        when(templateMapper.toResponse(saved)).thenReturn(expected);

        assertThat(templateService.create(request)).isEqualTo(expected);
        assertThat(mapped.isActive()).isFalse();
    }

    @Test
    void update_emailWithoutSubject_throwsBeforeLoadingEntity() {
        UUID id = UUID.randomUUID();
        TemplateRequest request = new TemplateRequest(TemplateType.EMAIL, "Boas-vindas", null, "Ola", null);

        assertThatThrownBy(() -> templateService.update(id, request))
                .isInstanceOf(BusinessException.class);

        verify(templateRepository, never()).findById(any());
        verify(templateRepository, never()).save(any(Template.class));
    }

    @Test
    void update_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        TemplateRequest request = new TemplateRequest(TemplateType.PROPOSAL, "Proposta", null, "Conteudo", null);
        when(templateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_softDeletedTemplate_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        TemplateRequest request = new TemplateRequest(TemplateType.PROPOSAL, "Proposta", null, "Conteudo", null);

        Template deleted = new Template();
        deleted.setId(id);
        deleted.setDeletedAt(java.time.Instant.now());
        when(templateRepository.findById(id)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> templateService.update(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existingTemplate_softDeletesAndAudits() {
        UUID id = UUID.randomUUID();
        Template template = new Template();
        template.setId(id);

        when(templateRepository.findById(id)).thenReturn(Optional.of(template));
        when(templateRepository.save(template)).thenReturn(template);

        templateService.delete(id);

        assertThat(template.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(template);
    }
}
