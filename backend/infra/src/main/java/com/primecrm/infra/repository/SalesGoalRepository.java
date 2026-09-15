package com.primecrm.infra.repository;

import com.primecrm.infra.entity.sales.SalesGoal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesGoalRepository extends JpaRepository<SalesGoal, UUID>, JpaSpecificationExecutor<SalesGoal> {

    Optional<SalesGoal> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByOwner_IdAndReferenceMonthAndDeletedAtIsNull(UUID ownerId, LocalDate referenceMonth);

    boolean existsByOwner_IdAndReferenceMonthAndIdNotAndDeletedAtIsNull(UUID ownerId, LocalDate referenceMonth,
            UUID excludingId);
}
