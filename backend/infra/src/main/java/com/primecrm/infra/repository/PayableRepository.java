package com.primecrm.infra.repository;

import com.primecrm.infra.entity.finance.Payable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PayableRepository extends JpaRepository<Payable, UUID>, JpaSpecificationExecutor<Payable> {

    Optional<Payable> findByIdAndDeletedAtIsNull(UUID id);
}
