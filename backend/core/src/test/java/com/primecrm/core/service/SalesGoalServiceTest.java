package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.sales.SalesGoalRequest;
import com.primecrm.core.dto.sales.SalesGoalResponse;
import com.primecrm.core.mapper.SalesGoalMapper;
import com.primecrm.core.service.support.CommercialReferenceResolver;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.sales.SalesGoal;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.SalesGoalRepository;
import com.primecrm.infra.repository.projection.AmountAggregate;
import com.primecrm.shared.exception.ConflictException;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalesGoalServiceTest {

    @Mock
    private SalesGoalRepository salesGoalRepository;
    @Mock
    private OpportunityRepository opportunityRepository;
    @Mock
    private SalesGoalMapper salesGoalMapper;
    @Mock
    private CommercialReferenceResolver referenceResolver;
    @Mock
    private AuditService auditService;

    private SalesGoalService salesGoalService;

    @BeforeEach
    void setUp() {
        salesGoalService = new SalesGoalService(salesGoalRepository, opportunityRepository, salesGoalMapper,
                referenceResolver, auditService);
    }

    @Test
    void create_normalizesReferenceMonthToFirstDayAndAudits() {
        UUID ownerId = UUID.randomUUID();
        SalesGoalRequest request = new SalesGoalRequest(ownerId, LocalDate.of(2026, 9, 15),
                new BigDecimal("10000.00"), null);
        SalesGoal goal = newGoal(ownerId, new BigDecimal("10000.00"));
        User owner = newUser(ownerId);

        when(salesGoalRepository.existsByOwner_IdAndReferenceMonthAndDeletedAtIsNull(ownerId,
                LocalDate.of(2026, 9, 1))).thenReturn(false);
        when(salesGoalMapper.toEntity(request)).thenReturn(goal);
        when(referenceResolver.user(ownerId)).thenReturn(owner);
        when(salesGoalRepository.save(goal)).thenReturn(goal);
        stubEmptyResponse(goal);
        stubRealizedAmount(ownerId, BigDecimal.ZERO);

        salesGoalService.create(request);

        assertThat(goal.getReferenceMonth()).isEqualTo(LocalDate.of(2026, 9, 1));
        verify(auditService).recordCreate(goal);
    }

    @Test
    void create_whenGoalAlreadyExistsForOwnerAndMonth_throwsConflict() {
        UUID ownerId = UUID.randomUUID();
        SalesGoalRequest request = new SalesGoalRequest(ownerId, LocalDate.of(2026, 9, 1),
                new BigDecimal("10000.00"), null);

        when(salesGoalRepository.existsByOwner_IdAndReferenceMonthAndDeletedAtIsNull(ownerId,
                LocalDate.of(2026, 9, 1))).thenReturn(true);

        assertThatThrownBy(() -> salesGoalService.create(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void findById_computesAchievementPercentFromRealizedAmount() {
        UUID ownerId = UUID.randomUUID();
        SalesGoal goal = newGoal(ownerId, new BigDecimal("1000.00"));
        goal.setReferenceMonth(LocalDate.of(2026, 9, 1));

        when(salesGoalRepository.findByIdAndDeletedAtIsNull(goal.getId())).thenReturn(Optional.of(goal));
        stubEmptyResponse(goal);
        AmountAggregate aggregate = amountAggregate(new BigDecimal("250.00"));
        when(opportunityRepository.summarizeClosedByOwnerBetween(eq(ownerId), eq(OpportunityOutcome.WON), any(),
                any())).thenReturn(aggregate);

        SalesGoalResponse response = salesGoalService.findById(goal.getId());

        assertThat(response.realizedAmount()).isEqualByComparingTo("250.00");
        assertThat(response.achievementPercent()).isEqualByComparingTo("25.00");
    }

    @Test
    void delete_marksTheGoalAsDeletedAndAudits() {
        SalesGoal goal = newGoal(UUID.randomUUID(), new BigDecimal("500.00"));
        when(salesGoalRepository.findByIdAndDeletedAtIsNull(goal.getId())).thenReturn(Optional.of(goal));

        salesGoalService.delete(goal.getId());

        assertThat(goal.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(goal);
    }

    @Test
    void findById_withUnknownId_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(salesGoalRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> salesGoalService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private void stubEmptyResponse(SalesGoal goal) {
        when(salesGoalMapper.toResponse(goal)).thenReturn(new SalesGoalResponse(goal.getId(), null,
                goal.getReferenceMonth(), goal.getTargetAmount(), null, null, goal.getNotes(), null, null));
    }

    private void stubRealizedAmount(UUID ownerId, BigDecimal amount) {
        when(opportunityRepository.summarizeClosedByOwnerBetween(eq(ownerId), eq(OpportunityOutcome.WON), any(),
                any())).thenReturn(amountAggregate(amount));
    }

    private AmountAggregate amountAggregate(BigDecimal totalAmount) {
        return new AmountAggregate() {
            @Override
            public long getItemCount() {
                return 1;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return totalAmount;
            }
        };
    }

    private User newUser(UUID id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private SalesGoal newGoal(UUID ownerId, BigDecimal targetAmount) {
        SalesGoal goal = new SalesGoal();
        goal.setId(UUID.randomUUID());
        goal.setOwner(newUser(ownerId));
        goal.setTargetAmount(targetAmount);
        goal.setReferenceMonth(LocalDate.now().withDayOfMonth(1));
        return goal;
    }
}
