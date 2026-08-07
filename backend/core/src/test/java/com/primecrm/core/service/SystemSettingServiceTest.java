package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.systemsettings.SystemSettingResponse;
import com.primecrm.core.dto.systemsettings.SystemSettingUpdateRequest;
import com.primecrm.core.mapper.SystemSettingMapper;
import com.primecrm.infra.entity.config.SystemSetting;
import com.primecrm.infra.repository.SystemSettingRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;
    @Mock
    private SystemSettingMapper systemSettingMapper;
    @Mock
    private AuditService auditService;

    private SystemSettingService systemSettingService;

    @BeforeEach
    void setUp() {
        systemSettingService = new SystemSettingService(systemSettingRepository, systemSettingMapper, auditService);
    }

    private SystemSetting setting(String key, String value) {
        SystemSetting setting = new SystemSetting();
        setting.setId(UUID.randomUUID());
        setting.setSettingKey(key);
        setting.setSettingValue(value);
        return setting;
    }

    @Test
    void updateByKey_existingKey_updatesValueAndAudits() {
        SystemSetting existing = setting("company.name", "Prime");
        SystemSettingUpdateRequest request = new SystemSettingUpdateRequest("Prime CRM");
        SystemSettingResponse expected =
                new SystemSettingResponse(existing.getId(), "company.name", "Prime CRM", null);
        Map<String, Object> previousState = Map.of("settingValue", "Prime");

        when(systemSettingRepository.findOne(any(Specification.class))).thenReturn(Optional.of(existing));
        when(auditService.snapshot(existing)).thenReturn(previousState);
        when(systemSettingRepository.save(existing)).thenReturn(existing);
        when(systemSettingMapper.toResponse(existing)).thenReturn(expected);

        assertThat(systemSettingService.updateByKey("company.name", request)).isEqualTo(expected);
        assertThat(existing.getSettingValue()).isEqualTo("Prime CRM");
        verify(auditService).recordUpdate(existing, previousState);
    }

    @Test
    void updateByKey_unknownKey_throwsResourceNotFoundWithoutCreatingSetting() {
        when(systemSettingRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                systemSettingService.updateByKey("nao.existe", new SystemSettingUpdateRequest("valor")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nao.existe");

        verify(systemSettingRepository, never()).save(any(SystemSetting.class));
        verify(auditService, never()).recordUpdate(any(), any());
    }

    @Test
    void findAll_returnsSettingsSortedByKey() {
        SystemSetting zeta = setting("zeta.key", "1");
        SystemSetting alpha = setting("alpha.key", "2");

        SystemSettingResponse zetaResponse = new SystemSettingResponse(zeta.getId(), "zeta.key", "1", null);
        SystemSettingResponse alphaResponse = new SystemSettingResponse(alpha.getId(), "alpha.key", "2", null);

        when(systemSettingRepository.findAll(any(Specification.class))).thenReturn(List.of(zeta, alpha));
        when(systemSettingMapper.toResponse(zeta)).thenReturn(zetaResponse);
        when(systemSettingMapper.toResponse(alpha)).thenReturn(alphaResponse);

        assertThat(systemSettingService.findAll()).containsExactly(alphaResponse, zetaResponse);
    }
}
