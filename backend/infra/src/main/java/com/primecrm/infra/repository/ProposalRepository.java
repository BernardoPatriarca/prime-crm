package com.primecrm.infra.repository;

import com.primecrm.infra.entity.proposal.Proposal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, UUID>, JpaSpecificationExecutor<Proposal> {

    Optional<Proposal> findByIdAndDeletedAtIsNull(UUID id);
}
