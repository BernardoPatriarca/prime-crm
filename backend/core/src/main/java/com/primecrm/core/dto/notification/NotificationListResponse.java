package com.primecrm.core.dto.notification;

import java.util.List;

public record NotificationListResponse(
        int total,
        List<NotificationResponse> items
) {
}
