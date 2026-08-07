package com.primecrm.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.holiday.HolidayRequest;
import com.primecrm.core.dto.holiday.HolidayResponse;
import com.primecrm.core.mapper.HolidayMapper;
import com.primecrm.infra.entity.config.Holiday;
import com.primecrm.infra.repository.HolidayRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private HolidayMapper holidayMapper;
    @Mock
    private AuditService auditService;

    private HolidayService holidayService;

    @BeforeEach
    void setUp() {
        holidayService = new HolidayService(holidayRepository, holidayMapper, auditService);
    }

    @Test
    void create_withoutFlags_defaultsToNationalAndActive() {
        HolidayRequest request = new HolidayRequest(LocalDate.of(2026, 9, 7), "Independencia", null, null);

        Holiday mapped = new Holiday();
        Holiday saved = new Holiday();
        saved.setId(UUID.randomUUID());
        HolidayResponse expected = new HolidayResponse(saved.getId(), request.holidayDate(), "Independencia", true, true);

        when(holidayMapper.toEntity(request)).thenReturn(mapped);
        when(holidayRepository.save(mapped)).thenReturn(saved);
        when(holidayMapper.toResponse(saved)).thenReturn(expected);

        assertThat(holidayService.create(request)).isEqualTo(expected);
        assertThat(mapped.isNational()).isTrue();
        assertThat(mapped.isActive()).isTrue();
        verify(auditService).recordCreate(saved);
    }

    @Test
    void create_withExplicitFlags_honoursRequestValues() {
        HolidayRequest request = new HolidayRequest(LocalDate.of(2026, 1, 25), "Aniversario da cidade", false, false);

        Holiday mapped = new Holiday();
        Holiday saved = new Holiday();
        saved.setId(UUID.randomUUID());

        when(holidayMapper.toEntity(request)).thenReturn(mapped);
        when(holidayRepository.save(mapped)).thenReturn(saved);

        holidayService.create(request);

        assertThat(mapped.isNational()).isFalse();
        assertThat(mapped.isActive()).isFalse();
    }

    @Test
    void update_existingHoliday_appliesChangesAndAudits() {
        UUID id = UUID.randomUUID();
        HolidayRequest request = new HolidayRequest(LocalDate.of(2026, 12, 25), "Natal", false, true);

        Holiday holiday = new Holiday();
        holiday.setId(id);
        holiday.setNational(true);
        Map<String, Object> previousState = Map.of("national", true);

        when(holidayRepository.findById(id)).thenReturn(Optional.of(holiday));
        when(auditService.snapshot(holiday)).thenReturn(previousState);
        when(holidayRepository.save(holiday)).thenReturn(holiday);

        holidayService.update(id, request);

        assertThat(holiday.isNational()).isFalse();
        assertThat(holiday.isActive()).isTrue();
        verify(auditService).recordUpdate(holiday, previousState);
    }

    @Test
    void update_unknownHoliday_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(holidayRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidayService.update(id, new HolidayRequest(LocalDate.now(), "X", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(holidayRepository, never()).save(any(Holiday.class));
    }

    @Test
    void findById_softDeletedHoliday_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        Holiday deleted = new Holiday();
        deleted.setId(id);
        deleted.setDeletedAt(Instant.now());

        when(holidayRepository.findById(id)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> holidayService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existingHoliday_softDeletesAndAudits() {
        UUID id = UUID.randomUUID();
        Holiday holiday = new Holiday();
        holiday.setId(id);

        when(holidayRepository.findById(id)).thenReturn(Optional.of(holiday));
        when(holidayRepository.save(holiday)).thenReturn(holiday);

        holidayService.delete(id);

        assertThat(holiday.getDeletedAt()).isNotNull();
        verify(auditService).recordDelete(holiday);
    }

    @Test
    void list_filteredByPeriod_mapsPageContent() {
        Pageable pageable = PageRequest.of(0, 10);
        Holiday holiday = new Holiday();
        holiday.setId(UUID.randomUUID());
        holiday.setHolidayDate(LocalDate.of(2026, 5, 1));
        holiday.setName("Dia do Trabalho");

        HolidayResponse expected = new HolidayResponse(holiday.getId(), holiday.getHolidayDate(),
                "Dia do Trabalho", true, true);

        when(holidayRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(holiday), pageable, 1));
        when(holidayMapper.toResponse(holiday)).thenReturn(expected);

        Page<HolidayResponse> page = holidayService.list(2026, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true, pageable);

        assertThat(page.getContent()).containsExactly(expected);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }
}
