package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.Contact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {

    Optional<Contact> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"customer", "department"})
    List<Contact> findByCustomer_IdInAndDeletedAtIsNull(Collection<UUID> customerIds);

    @EntityGraph(attributePaths = {"department"})
    List<Contact> findByCustomer_IdAndDeletedAtIsNullOrderByNameAsc(UUID customerId);

    long countByCustomer_IdAndDeletedAtIsNull(UUID customerId);

    List<Contact> findByCustomer_IdAndPrimaryContactIsTrueAndDeletedAtIsNull(UUID customerId);
}
