package com.primecrm.core.service;

import com.primecrm.core.audit.AuditService;
import com.primecrm.core.dto.holiday.HolidayRequest;
import com.primecrm.core.dto.holiday.HolidayResponse;
import com.primecrm.core.mapper.HolidayMapper;
import com.primecrm.core.specification.HolidaySpecifications;
import com.primecrm.core.specification.SpecificationUtils;
import com.primecrm.infra.entity.config.Holiday;
import com.primecrm.infra.repository.HolidayRepository;
import com.primecrm.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final HolidayMapper holidayMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<HolidayResponse> list(Integer year, LocalDate startDate, LocalDate endDate, Boolean active,
                                       Pageable pageable) {
        var spec = SpecificationUtils.<Holiday>and(
                HolidaySpecifications.notDeleted(),
                HolidaySpecifications.byYear(year),
                HolidaySpecifications.fromDate(startDate),
                HolidaySpecifications.toDate(endDate),
                HolidaySpecifications.hasActive(active)
        );
        return holidayRepository.findAll(spec, pageable).map(holidayMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public HolidayResponse findById(UUID id) {
        return holidayMapper.toResponse(getActiveOrThrow(id));
    }

    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        Holiday holiday = holidayMapper.toEntity(request);
        holiday.setNational(request.national() == null || request.national());
        holiday.setActive(request.active() == null || request.active());

        holiday = holidayRepository.save(holiday);
        auditService.recordCreate(holiday);
        return holidayMapper.toResponse(holiday);
    }

    @Transactional
    public HolidayResponse update(UUID id, HolidayRequest request) {
        Holiday holiday = getActiveOrThrow(id);
        Map<String, Object> previousState = auditService.snapshot(holiday);
        holidayMapper.updateEntity(holiday, request);
        if (request.national() != null) {
            holiday.setNational(request.national());
        }
        if (request.active() != null) {
            holiday.setActive(request.active());
        }

        holiday = holidayRepository.save(holiday);
        auditService.recordUpdate(holiday, previousState);
        return holidayMapper.toResponse(holiday);
    }

    @Transactional
    public void delete(UUID id) {
        Holiday holiday = getActiveOrThrow(id);
        holiday.setDeletedAt(Instant.now());
        holidayRepository.save(holiday);
        auditService.recordDelete(holiday);
    }

    private Holiday getActiveOrThrow(UUID id) {
        return holidayRepository.findById(id)
                .filter(h -> !h.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Feriado", id));
    }
}
