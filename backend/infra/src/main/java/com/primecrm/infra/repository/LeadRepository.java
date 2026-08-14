package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.Lead;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    Optional<Lead> findByIdAndDeletedAtIsNull(UUID id);

    List<Lead> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    List<Lead> findByConvertedCustomer_IdInAndDeletedAtIsNull(Collection<UUID> customerIds);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, UUID id);

    long countByDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    long countByDeletedAtIsNullAndConvertedAtGreaterThanEqualAndConvertedAtLessThan(Instant from, Instant to);

    List<Lead> findTop10ByOwnerIsNullAndConvertedAtIsNullAndDeletedAtIsNullOrderByCreatedAtDesc();
}
