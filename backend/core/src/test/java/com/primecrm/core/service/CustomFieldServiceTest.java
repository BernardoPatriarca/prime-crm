package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.customfield.CustomFieldRequest;
import com.primecrm.core.dto.customfield.CustomFieldResponse;
import com.primecrm.core.mapper.CustomFieldMapper;
import com.primecrm.infra.entity.config.CustomField;
import com.primecrm.infra.entity.config.FieldType;
import com.primecrm.infra.repository.CustomFieldRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomFieldServiceTest {

    @Mock
    private CustomFieldRepository customFieldRepository;
    @Mock
    private CustomFieldMapper customFieldMapper;
    @Mock
    private AuditService auditService;

    private CustomFieldService customFieldService;

    @BeforeEach
    void setUp() {
        customFieldService = new CustomFieldService(customFieldRepository, customFieldMapper, auditService);
    }

    private CustomFieldRequest request(FieldType fieldType, Map<String, Object> options) {
        return new CustomFieldRequest("LEAD", "segmento", "Segmento", fieldType, options, null, null, null);
    }

    @Test
    void create_selectWithoutOptions_throwsBusinessException() {
        assertThatThrownBy(() -> customFieldService.create(request(FieldType.SELECT, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("options");

        verify(customFieldRepository, never()).save(any(CustomField.class));
    }

    @Test
    void create_multiselectWithEmptyOptions_throwsBusinessException() {
        assertThatThrownBy(() -> customFieldService.create(request(FieldType.MULTISELECT, Map.of())))
                .isInstanceOf(BusinessException.class);

        verify(customFieldRepository, never()).save(any(CustomField.class));
    }

    @Test
    void create_textWithoutOptions_savesFieldWithDefaults() {
        CustomFieldRequest request = request(FieldType.TEXT, null);

        CustomField mapped = new CustomField();
        CustomField saved = new CustomField();
        saved.setId(UUID.randomUUID());
        CustomFieldResponse expected = new CustomFieldResponse(saved.getId(), "LEAD", "segmento", "Segmento",
                FieldType.TEXT, null, false, 0, true);

        when(customFieldMapper.toEntity(request)).thenReturn(mapped);
        when(customFieldRepository.save(mapped)).thenReturn(saved);
        when(customFieldMapper.toResponse(saved)).thenReturn(expected);

        assertThat(customFieldService.create(request)).isEqualTo(expected);
        assertThat(mapped.isRequired()).isFalse();
        assertThat(mapped.getDisplayOrder()).isZero();
        assertThat(mapped.isActive()).isTrue();
        verify(auditService).recordCreate(saved);
    }

    @Test
    void create_selectWithOptions_savesField() {
        CustomFieldRequest request = request(FieldType.SELECT, Map.of("values", "A,B"));

        CustomField mapped = new CustomField();
        CustomField saved = new CustomField();
        saved.setId(UUID.randomUUID());

        when(customFieldMapper.toEntity(request)).thenReturn(mapped);
        when(customFieldRepository.save(mapped)).thenReturn(saved);

        assertThatCode(() -> customFieldService.create(request)).doesNotThrowAnyException();
        verify(customFieldRepository).save(mapped);
    }

    @Test
    void update_selectWithoutOptions_throwsBeforeLoadingEntity() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> customFieldService.update(id, request(FieldType.SELECT, null)))
                .isInstanceOf(BusinessException.class);

        verify(customFieldRepository, never()).findById(any());
    }

    @Test
    void update_existingField_recordsAuditWithPreviousState() {
        UUID id = UUID.randomUUID();
        CustomFieldRequest request = new CustomFieldRequest("LEAD", "segmento", "Segmento", FieldType.TEXT, null,
                true, 5, false);

        CustomField existing = new CustomField();
        existing.setId(id);
        Map<String, Object> previousState = Map.of("label", "Antigo");

        when(customFieldRepository.findById(id)).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn(previousState);
        when(customFieldRepository.save(existing)).thenReturn(existing);

        customFieldService.update(id, request);

        assertThat(existing.isRequired()).isTrue();
        assertThat(existing.getDisplayOrder()).isEqualTo(5);
        assertThat(existing.isActive()).isFalse();
        verify(auditService).recordUpdate(existing, previousState);
    }

    @Test
    void findById_softDeletedField_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        CustomField deleted = new CustomField();
        deleted.setId(id);
        deleted.setDeletedAt(Instant.now());

        when(customFieldRepository.findById(id)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> customFieldService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existingField_softDeletesAndAudits() {
        UUID id = UUID.randomUUID();
        CustomField customField = new CustomField();
        customField.setId(id);

        when(customFieldRepository.findById(id)).thenReturn(Optional.of(customField));
        when(customFieldRepository.save(customField)).thenReturn(customField);

        customFieldService.delete(id);

        assertThat(customField.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(customField);
    }
}
