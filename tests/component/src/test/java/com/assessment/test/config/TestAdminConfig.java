package com.assessment.test.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Загружает тестовые учётные данные администратора из
 * {@code config/test-admin.properties}.
 * <p>
 * Эти данные отличаются от production-значений в {@code .env} / {@code .env.example}.
 * Для запуска тестов backend должен стартовать с соответствующей
 * {@code ADMIN_PASSWORD_HASH}, заданной в {@code start-backend.bat}.
 */
public class TestAdminConfig {

    private static final String PROPERTIES_FILE = "config/test-admin.properties";
    private static final String KEY_USERNAME = "admin.username";
    private static final String KEY_PASSWORD = "admin.password";

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = TestAdminConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new RuntimeException("Test admin properties not found: " + PROPERTIES_FILE);
            }
            PROPS.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test admin properties", e);
        }
    }

    public static String username() {
        return PROPS.getProperty(KEY_USERNAME);
    }

    public static String password() {
        return PROPS.getProperty(KEY_PASSWORD);
    }
}
