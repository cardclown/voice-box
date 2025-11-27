package com.example.voicebox.app.device.db;

/**
 * Centralizes database connection properties with sensible fallbacks so that the
 * application can run both inside Docker (service discovery) and directly on the
 * host machine without changing code.
 */
public final class DatabaseConfig {

    private DatabaseConfig() {}

    private static String resolve(String sysKey, String envKey, String defaultValue) {
        String sysValue = System.getProperty(sysKey);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return defaultValue;
    }

    public static String host() {
        return resolve("voicebox.db.host", "VOICEBOX_DB_HOST", "localhost");
    }

    public static String port() {
        return resolve("voicebox.db.port", "VOICEBOX_DB_PORT", "3306");
    }

    public static String database() {
        return resolve("voicebox.db.name", "VOICEBOX_DB_NAME", "voicebox_db");
    }

    public static String user() {
        return resolve("voicebox.db.user", "VOICEBOX_DB_USER", "root");
    }

    public static String password() {
        return resolve("voicebox.db.password", "VOICEBOX_DB_PASSWORD", "root");
    }

    public static String jdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf-8",
                host(),
                port(),
                database()
        );
    }
}

