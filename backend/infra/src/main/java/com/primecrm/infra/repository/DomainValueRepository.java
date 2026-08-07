package com.primecrm.infra.repository;

import com.primecrm.infra.entity.domain.DomainValue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DomainValueRepository
        extends JpaRepository<DomainValue, UUID>, JpaSpecificationExecutor<DomainValue> {
}
