package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.Customer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByIdAndDeletedAtIsNull(UUID id);

    List<Customer> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    List<Customer> findByParentCustomer_IdInAndDeletedAtIsNull(Collection<UUID> parentCustomerIds);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndIdNotAndDeletedAtIsNull(String code, UUID id);

    boolean existsByDocumentAndDeletedAtIsNull(String document);

    boolean existsByDocumentAndIdNotAndDeletedAtIsNull(String document, UUID id);
}
