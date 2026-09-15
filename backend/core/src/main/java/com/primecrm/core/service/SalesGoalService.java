package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.sales.SalesGoalListFilter;
import com.primecrm.core.dto.sales.SalesGoalRequest;
import com.primecrm.core.dto.sales.SalesGoalResponse;
import com.primecrm.core.mapper.SalesGoalMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.core.specification.SalesGoalSpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.sales.SalesGoal;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.SalesGoalRepository;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.shared.exception.ConflictException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SalesGoalService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final SalesGoalRepository salesGoalRepository;
    private final OpportunityRepository opportunityRepository;
    private final SalesGoalMapper salesGoalMapper;
    private final CommercialReferenceResolver referenceResolver;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<SalesGoalResponse> list(SalesGoalListFilter filter, Pageable pageable) {
        return salesGoalRepository.findAll(toSpecification(filter), pageable).map(this::enrich);
    }

    @Transactional(readOnly = true)
    public SalesGoalResponse findById(UUID id) {
        return enrich(getActiveOrThrow(id));
    }

    @Transactional
    public SalesGoalResponse create(SalesGoalRequest request) {
        LocalDate referenceMonth = firstDayOfMonth(request.referenceMonth());
        ensureAvailable(request.ownerUserId(), referenceMonth, null);

        SalesGoal goal = salesGoalMapper.toEntity(request);
        goal.setOwner(referenceResolver.user(request.ownerUserId()));
        goal.setReferenceMonth(referenceMonth);

        goal = salesGoalRepository.save(goal);
        auditService.recordCreate(goal);
        return enrich(goal);
    }

    @Transactional
    public SalesGoalResponse update(UUID id, SalesGoalRequest request) {
        SalesGoal goal = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(goal);

        LocalDate referenceMonth = firstDayOfMonth(request.referenceMonth());
        ensureAvailable(request.ownerUserId(), referenceMonth, id);

        salesGoalMapper.updateEntity(goal, request);
        goal.setOwner(referenceResolver.user(request.ownerUserId()));
        goal.setReferenceMonth(referenceMonth);

        goal = salesGoalRepository.save(goal);
        auditService.recordUpdate(goal, previousState);
        return enrich(goal);
    }

    @Transactional
    public void delete(UUID id) {
        SalesGoal goal = getActiveOrThrow(id);
        goal.setDeletedAt(Instant.now());
        salesGoalRepository.save(goal);
        auditService.recordDelete(goal);
    }

    private Specification<SalesGoal> toSpecification(SalesGoalListFilter filter) {
        return SpecificationUtils.and(
                SalesGoalSpecifications.notDeleted(),
                SalesGoalSpecifications.withReferencesFetched(),
                SalesGoalSpecifications.textSearch(filter.search()),
                SalesGoalSpecifications.hasOwner(filter.ownerUserId()),
                SalesGoalSpecifications.hasReferenceMonth(filter.referenceMonth()));
    }

    private SalesGoalResponse enrich(SalesGoal goal) {
        SalesGoalResponse base = salesGoalMapper.toResponse(goal);
        BigDecimal realized = realizedAmount(goal);
        BigDecimal achievement = rate(realized, goal.getTargetAmount());
        return new SalesGoalResponse(base.id(), base.owner(), base.referenceMonth(), base.targetAmount(), realized,
                achievement, base.notes(), base.createdAt(), base.updatedAt());
    }

    private BigDecimal realizedAmount(SalesGoal goal) {
        Instant from = goal.getReferenceMonth().atStartOfDay(BUSINESS_ZONE).toInstant();
        Instant to = goal.getReferenceMonth().plusMonths(1).atStartOfDay(BUSINESS_ZONE).toInstant();
        AmountAggregate aggregate = opportunityRepository.summarizeClosedByOwnerBetween(goal.getOwner().getId(),
                OpportunityOutcome.WON, from, to);
        return aggregate == null || aggregate.getTotalAmount() == null ? BigDecimal.ZERO : aggregate.getTotalAmount();
    }

    private BigDecimal rate(BigDecimal value, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(ONE_HUNDRED).divide(total, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private void ensureAvailable(UUID ownerUserId, LocalDate referenceMonth, UUID excludingId) {
        boolean taken = excludingId == null
                ? salesGoalRepository.existsByOwner_IdAndReferenceMonthAndDeletedAtIsNull(ownerUserId, referenceMonth)
                : salesGoalRepository.existsByOwner_IdAndReferenceMonthAndIdNotAndDeletedAtIsNull(ownerUserId,
                        referenceMonth, excludingId);
        if (taken) {
            throw new ConflictException("Ja existe uma meta cadastrada para este vendedor neste mes");
        }
    }

    private LocalDate firstDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    private SalesGoal getActiveOrThrow(UUID id) {
        return salesGoalRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Meta comercial", id));
    }
}
