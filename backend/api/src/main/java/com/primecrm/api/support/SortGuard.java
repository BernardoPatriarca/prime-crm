package com.primecrm.api.support;

import com.primecrm.shared.exception.BadRequestException;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class SortGuard {

    private static final Set<String> BLOCKED_PROPERTIES = Set.of(
            "passwordhash",
            "password",
            "tokenhash",
            "refreshtokenhash",
            "secret"
    );

    private SortGuard() {
    }

    public static Pageable requireSafeSort(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return pageable;
        }
        for (Sort.Order order : pageable.getSort()) {
            if (BLOCKED_PROPERTIES.contains(normalize(order.getProperty()))) {
                throw new BadRequestException("INVALID_SORT_PROPERTY",
                        "Ordenacao nao permitida para a propriedade informada");
            }
        }
        return pageable;
    }

    private static String normalize(String property) {
        return property.replace("_", "").replace(".", "").toLowerCase(Locale.ROOT);
    }
}
