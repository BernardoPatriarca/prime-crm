package com.primecrm.core.service;

import com.primecrm.core.audit.AuditChanges;
import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.commercial.CustomerRequest;
import com.primecrm.core.dto.commercial.CustomerResponse;
import com.primecrm.core.dto.commercial.LeadConvertRequest;
import com.primecrm.core.dto.commercial.LeadConvertResponse;
import com.primecrm.core.dto.commercial.LeadListFilter;
import com.primecrm.core.dto.commercial.LeadRequest;
import com.primecrm.core.dto.commercial.LeadResponse;
import com.primecrm.core.dto.commercial.OpportunityRequest;
import com.primecrm.core.dto.commercial.OpportunityResponse;
import com.primecrm.core.dto.common.DomainValueSummaryResponse;
import com.primecrm.core.mapper.CommercialSummaryMapper;
import com.primecrm.core.mapper.LeadMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.LeadSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.audit.AuditAction;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.LeadTag;
import com.primecrm.infra.entity.commercial.PersonType;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.LeadTagRepository;
import com.primecrm.shared.exception.BusinessException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LeadService {

    private static final String AUDIT_ENTITY = "Lead";

    private final LeadRepository leadRepository;
    private final LeadTagRepository leadTagRepository;
    private final CustomerRepository customerRepository;
    private final LeadMapper leadMapper;
    private final CommercialSummaryMapper summaryMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final CustomerService customerService;
    private final OpportunityService opportunityService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<LeadResponse> list(LeadListFilter filter, Pageable pageable) {
        var spec = SpecificationUtils.<Lead>and(
                LeadSpecifications.notDeleted(),
                LeadSpecifications.withReferencesFetched(),
                LeadSpecifications.textSearch(filter.search()),
                LeadSpecifications.hasOrigin(filter.originId()),
                LeadSpecifications.hasStatus(filter.statusId()),
                LeadSpecifications.hasPriority(filter.priorityId()),
                LeadSpecifications.hasOwner(filter.ownerUserId()),
                LeadSpecifications.hasPipeline(filter.pipelineId()),
                LeadSpecifications.hasStage(filter.stageId()),
                LeadSpecifications.hasActive(filter.active()),
                LeadSpecifications.expectedCloseFrom(filter.expectedCloseFrom()),
                LeadSpecifications.expectedCloseTo(filter.expectedCloseTo())
        );
        Page<Lead> page = leadRepository.findAll(spec, pageable);
        Map<UUID, List<DomainValueSummaryResponse>> tagsByLead = loadTagsByLead(page.getContent());
        return page.map(lead -> leadMapper.toResponse(lead, tagsByLead.getOrDefault(lead.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public LeadResponse findById(UUID id) {
        return toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public LeadResponse create(LeadRequest request) {
        Lead lead = leadMapper.toEntity(request);
        lead.setActive(request.active() == null || request.active());
        applyReferences(lead, request);

        lead = leadRepository.save(lead);
        replaceTags(lead, request.tagIds());
        auditService.recordCreate(lead);
        return toResponse(lead);
    }

    @Transactional
    public LeadResponse update(UUID id, LeadRequest request) {
        Lead lead = getActiveOrThrow(id);

        Map<String, Object> previousState = auditService.snapshot(lead);
        leadMapper.updateEntity(lead, request);
        if (request.active() != null) {
            lead.setActive(request.active());
        }
        applyReferences(lead, request);

        lead = leadRepository.save(lead);
        if (request.tagIds() != null) {
            replaceTags(lead, request.tagIds());
        }
        auditService.recordUpdate(lead, previousState);
        return toResponse(lead);
    }

    @Transactional
    public void delete(UUID id) {
        Lead lead = getActiveOrThrow(id);
        lead.setDeletedAt(Instant.now());
        leadRepository.save(lead);
        auditService.recordDelete(lead);
    }

    @Transactional
    public LeadConvertResponse convert(UUID id, LeadConvertRequest request) {
        Lead lead = getActiveOrThrow(id);
        if (lead.getConvertedAt() != null || lead.getConvertedCustomer() != null) {
            throw new BusinessException("LEAD_ALREADY_CONVERTED",
                    "Este lead ja foi convertido em cliente.");
        }

        CustomerResponse customer = customerService.create(buildCustomerRequest(lead, request));
        Customer convertedCustomer = customerRepository.findByIdAndDeletedAtIsNull(customer.id())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", customer.id()));

        lead.setConvertedCustomer(convertedCustomer);
        lead.setConvertedAt(Instant.now());
        lead = leadRepository.save(lead);

        OpportunityResponse opportunity = null;
        if (Boolean.TRUE.equals(request.createOpportunity())) {
            opportunity = opportunityService.create(buildOpportunityRequest(lead, request, convertedCustomer));
        }

        auditService.recordChange(AuditAction.UPDATE, AUDIT_ENTITY, lead.getId(), Map.of(
                "convertedCustomer", AuditChanges.of(null, convertedCustomer.getId()),
                "convertedAt", AuditChanges.of(null, lead.getConvertedAt()),
                "opportunityCreated", opportunity != null));

        return new LeadConvertResponse(toResponse(lead), customer, opportunity);
    }

    private CustomerRequest buildCustomerRequest(Lead lead, LeadConvertRequest request) {
        boolean company = StringUtils.hasText(lead.getCompanyName());
        String name = company ? lead.getCompanyName() : lead.getName();
        String tradeName = company ? lead.getName() : null;
        UUID ownerUserId = request.ownerUserId() != null
                ? request.ownerUserId()
                : (lead.getOwner() == null ? null : lead.getOwner().getId());
        UUID originId = lead.getOrigin() == null ? null : lead.getOrigin().getId();
        List<UUID> tagIds = leadTagRepository.findByLead_IdIn(List.of(lead.getId())).stream()
                .map(link -> link.getDomainValue().getId())
                .toList();

        return CustomerRequest.builder()
                .name(name)
                .tradeName(tradeName)
                .personType(company ? PersonType.JURIDICA : PersonType.FISICA)
                .clientTypeId(request.clientTypeId())
                .segmentId(request.segmentId())
                .originId(originId)
                .ownerUserId(ownerUserId)
                .phone(lead.getPhone())
                .mobile(lead.getMobile())
                .email(lead.getEmail())
                .notes(lead.getNotes())
                .tagIds(tagIds)
                .active(Boolean.TRUE)
                .build();
    }

    private OpportunityRequest buildOpportunityRequest(Lead lead, LeadConvertRequest request, Customer customer) {
        UUID pipelineId = request.pipelineId() != null
                ? request.pipelineId()
                : (lead.getPipeline() == null ? null : lead.getPipeline().getId());
        if (pipelineId == null) {
            throw new BusinessException("PIPELINE_REQUIRED",
                    "Informe o funil para criar a oportunidade na conversao do lead.");
        }
        UUID stageId = request.stageId() != null
                ? request.stageId()
                : (lead.getStage() == null ? null : lead.getStage().getId());

        return OpportunityRequest.builder()
                .title(StringUtils.hasText(request.opportunityTitle())
                        ? request.opportunityTitle()
                        : lead.getName())
                .customerId(customer.getId())
                .pipelineId(pipelineId)
                .stageId(stageId)
                .amount(request.amount() != null ? request.amount() : lead.getEstimatedValue())
                .probability(lead.getProbability())
                .ownerUserId(lead.getOwner() == null ? null : lead.getOwner().getId())
                .expectedCloseDate(request.expectedCloseDate() != null
                        ? request.expectedCloseDate()
                        : lead.getExpectedCloseDate())
                .sourceLeadId(lead.getId())
                .notes(lead.getNotes())
                .build();
    }

    private void applyReferences(Lead lead, LeadRequest request) {
        lead.setOrigin(referenceResolver.domainValue(request.originId(), "Origem"));
        lead.setStatus(referenceResolver.domainValue(request.statusId(), "Status"));
        lead.setPriority(referenceResolver.domainValue(request.priorityId(), "Prioridade"));
        lead.setOwner(referenceResolver.user(request.ownerUserId()));
        lead.setPipeline(referenceResolver.pipeline(request.pipelineId()));
        lead.setStage(referenceResolver.stage(request.stageId()));
    }

    private void replaceTags(Lead lead, List<UUID> tagIds) {
        leadTagRepository.deleteByLead_Id(lead.getId());
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<DomainValue> tags = referenceResolver.domainValues(tagIds, "Tags do lead");
        List<LeadTag> links = tags.stream().map(tag -> {
            LeadTag link = new LeadTag();
            link.setLead(lead);
            link.setDomainValue(tag);
            return link;
        }).toList();
        leadTagRepository.saveAll(links);
    }

    private Lead getActiveOrThrow(UUID id) {
        return leadRepository.findById(id)
                .filter(lead -> !lead.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lead", id));
    }

    private LeadResponse toResponse(Lead lead) {
        return leadMapper.toResponse(lead, loadTagsByLead(List.of(lead)).getOrDefault(lead.getId(), List.of()));
    }

    private Map<UUID, List<DomainValueSummaryResponse>> loadTagsByLead(List<Lead> leads) {
        if (leads.isEmpty()) {
            return Map.of();
        }
        List<UUID> leadIds = leads.stream().map(Lead::getId).toList();
        return leadTagRepository.findByLead_IdIn(leadIds).stream()
                .collect(Collectors.groupingBy(link -> link.getLead().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(link -> summaryMapper.toDomainValueSummary(link.getDomainValue()),
                                Collectors.toList())));
    }
}
