package com.primecrm.api.performance;

import java.sql.Connection;
import java.sql.DriverManager;

public final class LocalPostgresCondition {

    private static final int PROBE_TIMEOUT_SECONDS = 2;

    private LocalPostgresCondition() {
    }

    public static boolean isReachable() {
        DriverManager.setLoginTimeout(PROBE_TIMEOUT_SECONDS);
        try (Connection connection = DriverManager.getConnection(url(), user(), password())) {
            return connection.isValid(PROBE_TIMEOUT_SECONDS);
        } catch (Exception ex) {
            return false;
        }
    }

    private static String url() {
        return "jdbc:postgresql://" + env("DB_HOST", "localhost") + ":" + env("DB_PORT", "5432")
                + "/" + env("DB_NAME", "primecrm");
    }

    private static String user() {
        return env("DB_USER", "postgres");
    }

    private static String password() {
        return env("DB_PASSWORD", "1234");
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
