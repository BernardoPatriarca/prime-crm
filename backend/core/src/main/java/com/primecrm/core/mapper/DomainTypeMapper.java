package com.primecrm.core.mapper;

import com.primecrm.core.dto.domain.DomainTypeResponse;
import com.primecrm.infra.entity.domain.DomainType;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DomainTypeMapper {

    DomainTypeResponse toResponse(DomainType domainType);

    List<DomainTypeResponse> toResponseList(List<DomainType> domainTypes);
}
