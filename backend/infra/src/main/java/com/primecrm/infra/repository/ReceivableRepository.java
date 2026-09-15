package com.primecrm.infra.repository;

import com.primecrm.infra.entity.finance.Receivable;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, UUID>, JpaSpecificationExecutor<Receivable> {

    Optional<Receivable> findByIdAndDeletedAtIsNull(UUID id);
}
