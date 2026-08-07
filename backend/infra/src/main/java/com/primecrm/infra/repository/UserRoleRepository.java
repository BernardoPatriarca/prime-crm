package com.primecrm.infra.repository;

import com.primecrm.infra.entity.auth.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID>, JpaSpecificationExecutor<UserRole> {

    @EntityGraph(attributePaths = {"user", "role"})
    List<UserRole> findByUser_IdIn(Collection<UUID> userIds);
}
