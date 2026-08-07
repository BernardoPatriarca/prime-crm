package com.primecrm.infra.repository;

import com.primecrm.infra.entity.commercial.LeadTag;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadTagRepository extends JpaRepository<LeadTag, UUID>, JpaSpecificationExecutor<LeadTag> {

    @EntityGraph(attributePaths = {"lead", "domainValue"})
    List<LeadTag> findByLead_IdIn(Collection<UUID> leadIds);

    void deleteByLead_Id(UUID leadId);
}
