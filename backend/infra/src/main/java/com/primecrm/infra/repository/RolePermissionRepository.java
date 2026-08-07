package com.primecrm.infra.repository;

import com.primecrm.infra.entity.auth.RolePermission;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository
        extends JpaRepository<RolePermission, UUID>, JpaSpecificationExecutor<RolePermission> {

    @EntityGraph(attributePaths = {"role", "permission"})
    List<RolePermission> findByRole_IdIn(Collection<UUID> roleIds);
}
