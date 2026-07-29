package com.assessment.test.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Разделяемое состояние между шагами одного сценария.
 * <p>
 * Хранит переменные, созданные в процессе сценария (competencyId, employeeId, token и т.д.).
 * Все поля — строки для простоты и потокобезопасности (static-доступ через нить).
 * Поскольку тесты идут последовательно, используется singleton на run.
 */
public class ScenarioContext {

    private static final ThreadLocal<ScenarioContext> INSTANCE = ThreadLocal.withInitial(ScenarioContext::new);

    private final Map<String, String> variables = new HashMap<>();
    private String lastResponseBody;
    private int lastStatusCode;

    public static ScenarioContext get() {
        return INSTANCE.get();
    }

    public static void reset() {
        INSTANCE.remove();
    }

    public void setVar(String key, String value) {
        variables.put(key, value);
    }

    public String getVar(String key) {
        return variables.get(key);
    }

    public void setLastResponse(String body, int statusCode) {
        this.lastResponseBody = body;
        this.lastStatusCode = statusCode;
    }

    public String lastResponseBody() {
        return lastResponseBody;
    }

    public int lastStatusCode() {
        return lastStatusCode;
    }
}
