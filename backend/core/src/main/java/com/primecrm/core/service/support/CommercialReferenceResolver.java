package com.primecrm.core.service.support;

import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.commercial.Contact;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.ContactRepository;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.DomainValueRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.infra.repository.UserRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommercialReferenceResolver {

    private final DomainValueRepository domainValueRepository;
    private final UserRepository userRepository;
    private final PipelineRepository pipelineRepository;
    private final PipelineStageRepository pipelineStageRepository;
    private final CustomerRepository customerRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;

    public DomainValue domainValue(UUID id, String label) {
        if (id == null) {
            return null;
        }
        return domainValueRepository.findById(id)
                .filter(value -> !value.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(label, id));
    }

    public List<DomainValue> domainValues(Collection<UUID> ids, String label) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> distinctIds = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        List<DomainValue> found = domainValueRepository.findAllById(distinctIds).stream()
                .filter(value -> !value.isDeleted())
                .toList();
        if (found.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("Um ou mais valores de dominio informados nao foram encontrados: "
                    + label);
        }
        return found;
    }

    public User user(UUID id) {
        if (id == null) {
            return null;
        }
        return userRepository.findById(id)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    public Pipeline pipeline(UUID id) {
        if (id == null) {
            return null;
        }
        return pipelineRepository.findById(id)
                .filter(pipeline -> !pipeline.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Funil", id));
    }

    public PipelineStage stage(UUID id) {
        if (id == null) {
            return null;
        }
        return pipelineStageRepository.findById(id)
                .filter(stage -> !stage.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Etapa de funil", id));
    }

    public PipelineStage firstStageOf(UUID pipelineId) {
        return pipelineStageRepository
                .findByPipeline_IdInAndDeletedAtIsNullOrderByDisplayOrderAsc(List.of(pipelineId)).stream()
                .min(java.util.Comparator.comparingInt(PipelineStage::getDisplayOrder))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "O funil informado nao possui etapas cadastradas: " + pipelineId));
    }

    public Customer customer(UUID id) {
        if (id == null) {
            return null;
        }
        return customerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
    }

    public Contact contact(UUID id) {
        if (id == null) {
            return null;
        }
        return contactRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contato", id));
    }

    public Lead lead(UUID id) {
        if (id == null) {
            return null;
        }
        return leadRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", id));
    }
}
