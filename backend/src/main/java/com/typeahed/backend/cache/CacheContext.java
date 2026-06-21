package com.typeahed.backend.cache;

public class CacheContext {

    private static final ThreadLocal<Boolean> cacheHitThreadLocal = new ThreadLocal<>();

    public static void setCacheHit(Boolean hit) {
        cacheHitThreadLocal.set(hit);
    }

    public static Boolean getCacheHit() {
        Boolean value = cacheHitThreadLocal.get();
        return value != null ? value : false;
    }

    public static void clear() {
        cacheHitThreadLocal.remove();
    }
}
