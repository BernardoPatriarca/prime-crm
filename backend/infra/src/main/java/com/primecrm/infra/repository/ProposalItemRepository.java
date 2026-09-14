package com.primecrm.infra.repository;

import com.primecrm.infra.entity.proposal.ProposalItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProposalItemRepository extends JpaRepository<ProposalItem, UUID> {

    List<ProposalItem> findByProposal_IdAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID proposalId);

    Optional<ProposalItem> findByIdAndProposal_IdAndDeletedAtIsNull(UUID id, UUID proposalId);

    long countByProposal_IdAndDeletedAtIsNull(UUID proposalId);
}
