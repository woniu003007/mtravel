package com.mtravel.platform.tenant;

public final class TenantContextHolder {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId(Long defaultTenantId) {
        Long tenantId = CURRENT_TENANT.get();
        return tenantId == null ? defaultTenantId : tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

