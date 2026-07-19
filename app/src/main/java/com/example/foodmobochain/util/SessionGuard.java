package com.example.foodmobochain.util;

public final class SessionGuard {
    private static final long TIMEOUT_MS = 15L * 60L * 1000L;
    private static long lastInteraction = System.currentTimeMillis();

    private SessionGuard() { }

    public static void touch() {
        lastInteraction = System.currentTimeMillis();
    }

    public static boolean expired() {
        return System.currentTimeMillis() - lastInteraction > TIMEOUT_MS;
    }
}
