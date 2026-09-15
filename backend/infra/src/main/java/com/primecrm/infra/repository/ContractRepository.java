package com.primecrm.infra.repository;

import com.primecrm.infra.entity.contract.Contract;
import com.primecrm.infra.entity.contract.ContractStatus;
import com.primecrm.infra.repository.projection.AmountAggregate;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID>, JpaSpecificationExecutor<Contract> {

    Optional<Contract> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select count(c) as itemCount, coalesce(sum(c.recurringAmount), 0) as totalAmount
            from Contract c
            where c.deletedAt is null and c.status = :status
            """)
    AmountAggregate summarizeByStatus(@Param("status") ContractStatus status);

    @Query("""
            select count(c) as itemCount, coalesce(sum(c.recurringAmount), 0) as totalAmount
            from Contract c
            where c.deletedAt is null and c.status = :status and c.endDate is not null
              and c.endDate >= :from and c.endDate < :to
            """)
    AmountAggregate summarizeExpiringBetween(@Param("status") ContractStatus status, @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
