package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.common.ReorderItem;
import com.primecrm.core.dto.common.ReorderRequest;
import com.primecrm.core.dto.domain.DomainValueRequest;
import com.primecrm.core.dto.domain.DomainValueResponse;
import com.primecrm.core.mapper.DomainValueMapper;
import com.primecrm.infra.entity.domain.DomainType;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.DomainValueRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DomainValueServiceTest {

    @Mock
    private DomainValueRepository domainValueRepository;
    @Mock
    private DomainValueMapper domainValueMapper;
    @Mock
    private DomainTypeService domainTypeService;
    @Mock
    private AuditService auditService;

    private DomainValueService domainValueService;

    @BeforeEach
    void setUp() {
        domainValueService = new DomainValueService(domainValueRepository, domainValueMapper, domainTypeService,
                auditService);
    }

    @Test
    void create_withUnknownDomainTypeCode_throwsResourceNotFound() {
        DomainValueRequest request = new DomainValueRequest("UNKNOWN_TYPE", "PF", "Pessoa Fisica", null, null, null,
                null, null, null);

        when(domainTypeService.getByCodeOrThrow("UNKNOWN_TYPE"))
                .thenThrow(new ResourceNotFoundException("Tipo de dominio nao encontrado: UNKNOWN_TYPE"));

        assertThatThrownBy(() -> domainValueService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(domainValueRepository, never()).save(any(DomainValue.class));
    }

    @Test
    void create_withKnownDomainTypeCode_savesValue() {
        DomainValueRequest request = new DomainValueRequest("CLIENT_TYPE", "PF", "Pessoa Fisica", null, null, null,
                2, null, null);

        DomainType domainType = new DomainType();
        domainType.setId(UUID.randomUUID());
        domainType.setCode("CLIENT_TYPE");
        domainType.setLabel("Tipo de Cliente");

        DomainValue mappedEntity = new DomainValue();
        DomainValue savedEntity = new DomainValue();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setDomainType(domainType);
        savedEntity.setName("Pessoa Fisica");

        DomainValueResponse expectedResponse = new DomainValueResponse(savedEntity.getId(), "CLIENT_TYPE",
                "Tipo de Cliente", "PF", "Pessoa Fisica", null, null, null, 2, null, true);

        when(domainTypeService.getByCodeOrThrow("CLIENT_TYPE")).thenReturn(domainType);
        when(domainValueMapper.toEntity(request)).thenReturn(mappedEntity);
        when(domainValueRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(domainValueMapper.toResponse(savedEntity)).thenReturn(expectedResponse);

        DomainValueResponse response = domainValueService.create(request);

        assertThat(response).isEqualTo(expectedResponse);
        assertThat(mappedEntity.getDomainType()).isEqualTo(domainType);
        assertThat(mappedEntity.getDisplayOrder()).isEqualTo(2);
        assertThat(mappedEntity.isActive()).isTrue();
        verify(domainValueRepository).save(mappedEntity);
    }

    @Test
    void findById_unknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(domainValueRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> domainValueService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reorder_withIdNotFound_throwsResourceNotFound() {
        UUID knownId = UUID.randomUUID();
        UUID unknownId = UUID.randomUUID();
        ReorderRequest request = new ReorderRequest(List.of(
                new ReorderItem(knownId, 1),
                new ReorderItem(unknownId, 2)
        ));

        DomainValue knownEntity = new DomainValue();
        knownEntity.setId(knownId);

        when(domainValueRepository.findAllById(any())).thenReturn(List.of(knownEntity));

        assertThatThrownBy(() -> domainValueService.reorder(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(domainValueRepository, never()).saveAll(any());
    }
}
