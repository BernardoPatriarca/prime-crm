package com.primecrm.core.service;

import com.primecrm.core.cache.CacheNames;
import com.primecrm.core.dto.domain.DomainTypeResponse;
import com.primecrm.core.mapper.DomainTypeMapper;
import com.primecrm.core.specification.DomainTypeSpecifications;
import com.primecrm.infra.entity.domain.DomainType;
import com.primecrm.infra.repository.DomainTypeRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DomainTypeService {

    private final DomainTypeRepository domainTypeRepository;
    private final DomainTypeMapper domainTypeMapper;

    @Cacheable(CacheNames.DOMAIN_TYPES)
    @Transactional(readOnly = true)
    public List<DomainTypeResponse> findAll() {
        return domainTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(DomainType::getLabel))
                .map(domainTypeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DomainType getByCodeOrThrow(String code) {
        return domainTypeRepository.findOne(DomainTypeSpecifications.byCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de dominio nao encontrado: " + code));
    }
}
