package com.primecrm.core.dto.holiday;

import java.time.LocalDate;
import java.util.UUID;

public record HolidayResponse(
        UUID id,
        LocalDate holidayDate,
        String name,
        boolean national,
        boolean active
) {
}
