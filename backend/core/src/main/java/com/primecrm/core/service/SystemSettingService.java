package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.systemsettings.SystemSettingResponse;
import com.primecrm.core.dto.systemsettings.SystemSettingUpdateRequest;
import com.primecrm.core.mapper.SystemSettingMapper;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.core.specification.SystemSettingSpecifications;
import com.primecrm.infra.entity.config.SystemSetting;
import com.primecrm.infra.repository.SystemSettingRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingMapper systemSettingMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SystemSettingResponse> findAll() {
        List<SystemSetting> settings = systemSettingRepository.findAll(SystemSettingSpecifications.notDeleted());
        return settings.stream()
                .sorted(Comparator.comparing(SystemSetting::getSettingKey))
                .map(systemSettingMapper::toResponse)
                .toList();
    }

    @Transactional
    public SystemSettingResponse updateByKey(String key, SystemSettingUpdateRequest request) {
        SystemSetting setting = getByKeyOrThrow(key);
        Map<String, Object> previousState = auditService.snapshot(setting);
        setting.setSettingValue(request.value());
        setting = systemSettingRepository.save(setting);
        auditService.recordUpdate(setting, previousState);
        return systemSettingMapper.toResponse(setting);
    }

    private SystemSetting getByKeyOrThrow(String key) {
        var spec = SpecificationUtils.<SystemSetting>and(
                SystemSettingSpecifications.notDeleted(),
                SystemSettingSpecifications.byKey(key)
        );
        return systemSettingRepository.findOne(spec)
                .orElseThrow(() -> new ResourceNotFoundException("Configuracao com chave '" + key + "' nao encontrada"));
    }
}
