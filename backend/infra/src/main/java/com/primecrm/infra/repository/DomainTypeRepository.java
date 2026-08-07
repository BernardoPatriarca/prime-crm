package com.primecrm.infra.repository;

import com.primecrm.infra.entity.domain.DomainType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainTypeRepository extends JpaRepository<DomainType, UUID>, JpaSpecificationExecutor<DomainType> {
}
