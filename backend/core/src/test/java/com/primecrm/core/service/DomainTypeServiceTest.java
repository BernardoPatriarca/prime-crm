package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.primecrm.core.dto.domain.DomainTypeResponse;
import com.primecrm.core.mapper.DomainTypeMapper;
import com.primecrm.infra.entity.domain.DomainType;
import com.primecrm.infra.repository.DomainTypeRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DomainTypeServiceTest {

    @Mock
    private DomainTypeRepository domainTypeRepository;
    @Mock
    private DomainTypeMapper domainTypeMapper;

    private DomainTypeService domainTypeService;

    @BeforeEach
    void setUp() {
        domainTypeService = new DomainTypeService(domainTypeRepository, domainTypeMapper);
    }

    private DomainType domainType(String code, String label) {
        DomainType domainType = new DomainType();
        domainType.setId(UUID.randomUUID());
        domainType.setCode(code);
        domainType.setLabel(label);
        return domainType;
    }

    @Test
    void findAll_mapsEveryTypeSortedByLabel() {
        DomainType origin = domainType("LEAD_ORIGIN", "Origem do Lead");
        DomainType clientType = domainType("CLIENT_TYPE", "Tipo de Cliente");

        DomainTypeResponse originResponse =
                new DomainTypeResponse(origin.getId(), "LEAD_ORIGIN", "Origem do Lead", true, true, true);
        DomainTypeResponse clientTypeResponse =
                new DomainTypeResponse(clientType.getId(), "CLIENT_TYPE", "Tipo de Cliente", true, true, true);

        when(domainTypeRepository.findAll()).thenReturn(List.of(clientType, origin));
        when(domainTypeMapper.toResponse(origin)).thenReturn(originResponse);
        when(domainTypeMapper.toResponse(clientType)).thenReturn(clientTypeResponse);

        assertThat(domainTypeService.findAll()).containsExactly(originResponse, clientTypeResponse);
    }

    @Test
    void getByCodeOrThrow_knownCode_returnsEntity() {
        DomainType clientType = domainType("CLIENT_TYPE", "Tipo de Cliente");
        when(domainTypeRepository.findOne(any(Specification.class))).thenReturn(Optional.of(clientType));

        assertThat(domainTypeService.getByCodeOrThrow("CLIENT_TYPE")).isEqualTo(clientType);
    }

    @Test
    void getByCodeOrThrow_unknownCode_throwsResourceNotFound() {
        when(domainTypeRepository.findOne(any(Specification.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> domainTypeService.getByCodeOrThrow("NAO_EXISTE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("NAO_EXISTE");
    }
}
