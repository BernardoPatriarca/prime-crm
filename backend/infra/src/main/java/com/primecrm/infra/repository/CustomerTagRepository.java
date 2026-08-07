package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.CustomerTag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerTagRepository
        extends JpaRepository<CustomerTag, UUID>, JpaSpecificationExecutor<CustomerTag> {

    @EntityGraph(attributePaths = {"customer", "domainValue"})
    List<CustomerTag> findByCustomer_IdIn(Collection<UUID> customerIds);

    void deleteByCustomer_Id(UUID customerId);
}
