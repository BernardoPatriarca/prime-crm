package com.primecrm.core.dto.commercial;

import com.primecrm.infra.entity.commercial.PersonType;
import java.util.List;
import java.util.UUID;

public record CustomerListFilter(
        String search,
        PersonType personType,
        UUID clientTypeId,
        UUID segmentId,
        UUID ownerUserId,
        Boolean active,
        List<UUID> tagIds
) {
}
