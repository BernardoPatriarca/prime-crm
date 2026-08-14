package com.primecrm.core.dto.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        NotificationType type,
        NotificationSeverity severity,
        UUID referenceId,
        String title,
        String description,
        String link,
        Instant date
) {
}
