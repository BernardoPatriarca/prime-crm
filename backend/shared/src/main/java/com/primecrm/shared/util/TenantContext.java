package com.primecrm.shared.util;

import java.util.UUID;

public final class TenantContext {

    public static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final ThreadLocal<UUID> CURRENT_TENANT = ThreadLocal.withInitial(() -> DEFAULT_TENANT_ID);

    private TenantContext() {
    }

    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(tenantId == null ? DEFAULT_TENANT_ID : tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
