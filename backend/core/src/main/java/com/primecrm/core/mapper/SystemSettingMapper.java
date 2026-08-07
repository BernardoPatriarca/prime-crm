package com.primecrm.core.mapper;

import com.primecrm.core.dto.systemsettings.SystemSettingResponse;
import com.primecrm.infra.entity.config.SystemSetting;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SystemSettingMapper {

    SystemSettingResponse toResponse(SystemSetting systemSetting);

    List<SystemSettingResponse> toResponseList(List<SystemSetting> systemSettings);
}
