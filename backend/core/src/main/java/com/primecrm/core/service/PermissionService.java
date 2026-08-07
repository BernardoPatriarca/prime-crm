package com.primecrm.core.service;

import com.primecrm.core.cache.CacheNames;
import com.primecrm.core.dto.permission.PermissionResponse;
import com.primecrm.core.mapper.PermissionMapper;
import com.primecrm.infra.repository.PermissionRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Cacheable(CacheNames.PERMISSIONS)
    @Transactional(readOnly = true)
    public List<PermissionResponse> findAll() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(p -> p.getModule() + p.getAction()))
                .map(permissionMapper::toResponse)
                .toList();
    }
}
