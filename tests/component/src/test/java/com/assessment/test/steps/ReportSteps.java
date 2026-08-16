package com.assessment.test.steps;

import com.assessment.test.client.TestHttpClient;
import com.assessment.test.config.TestAdminConfig;
import com.assessment.test.context.ScenarioContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import okhttp3.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportSteps {

    private static final TestHttpClient adminClient = new TestHttpClient();
    private static final TestHttpClient employeeClient = new TestHttpClient();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)}");
    private static final Pattern SESSION_UUID_PATTERN = Pattern.compile("/session/([0-9a-fA-F-]+)");
    private Response lastResponse;

    @Given("существует завершённая сессия")
    public void completedSessionExists() {
        setupFullSession(true);
    }

    @Given("существует активная сессия \\(не завершённая)")
    public void activeSessionExists() {
        setupFullSession(false);
    }

    @When("отправляю GET {string} как сотрудник")
    public void getAsEmployee(String path) {
        String resolvedPath = resolvePath(path);
        lastResponse = employeeClient.employeeGet(resolvedPath);
        ScenarioContext.get().setLastResponse(
                employeeClient.body(lastResponse),
                employeeClient.statusCode(lastResponse));
    }

    @When("отправляю GET {string} как администратор")
    public void getAsAdmin(String path) {
        String resolvedPath = resolvePath(path);
        lastResponse = adminClient.adminGet(resolvedPath);
        ScenarioContext.get().setLastResponse(
                adminClient.body(lastResponse),
                adminClient.statusCode(lastResponse));
    }

    @Then("статус ответа отчёта {int}")
    public void reportStatusCodeIs(int expected) {
        assertThat(ScenarioContext.get().lastStatusCode()).isEqualTo(expected);
    }

    @Then("JSON отчёта содержит {string}")
    public void reportJsonContains(String jsonPathExpr) {
        String body = ScenarioContext.get().lastResponseBody();
        Object value = JsonPath.read(body, jsonPathExpr);
        assertThat(value).isNotNull();
    }

    private void setupFullSession(boolean complete) {
        Map<String, String> loginParams = Map.of("username", TestAdminConfig.username(), "password", TestAdminConfig.password());
        Response loginResp = adminClient.adminLogin(loginParams);
        assertThat(adminClient.statusCode(loginResp)).isEqualTo(200);

        String uniqueName = "Java Report " + UUID.randomUUID().toString().substring(0, 8);
        Response compResp = adminClient.adminPost("/api/admin/competencies",
                Map.of("name", uniqueName, "description", "Report test"));
        assertThat(adminClient.statusCode(compResp)).isEqualTo(201);
        String competencyId = JsonPath.read(adminClient.body(compResp), "$.id");

        Response secResp = adminClient.adminPost("/api/admin/competencies/" + competencyId + "/sections",
                Map.of("name", "Core"));
        assertThat(adminClient.statusCode(secResp)).isEqualTo(201);
        String sectionId = JsonPath.read(adminClient.body(secResp), "$.id");

        Response topicResp = adminClient.adminPost("/api/admin/sections/" + sectionId + "/topics",
                Map.of("name", "Basics"));
        assertThat(adminClient.statusCode(topicResp)).isEqualTo(201);
        String topicId = JsonPath.read(adminClient.body(topicResp), "$.id");

        Response genResp = adminClient.adminPost(
                "/api/admin/competencies/" + competencyId + "/questions/generate",
                Map.of("count", 1, "difficulty", "MIDDLE"));
        assertThat(adminClient.statusCode(genResp)).isIn(200, 202);

        Response empResp = adminClient.adminPost("/api/admin/employees",
                Map.of("fullName", "Report Test", "email", "report@test.com", "competencyId", competencyId));
        assertThat(adminClient.statusCode(empResp)).isEqualTo(201);
        String employeeId = JsonPath.read(adminClient.body(empResp), "$.id");

        Response tokenResp = adminClient.adminPost("/api/admin/employees/" + employeeId + "/invite", "");
        assertThat(adminClient.statusCode(tokenResp)).isEqualTo(200);
        String tokenPath = adminClient.body(tokenResp).replace("\"", "").trim();
        String token = tokenPath.substring(tokenPath.lastIndexOf('/') + 1);

        Response inviteResp = employeeClient.openInvite(token);
        String location = employeeClient.header(inviteResp, "Location");
        Matcher matcher = SESSION_UUID_PATTERN.matcher(location != null ? location : "");
        assertThat(matcher.find()).isTrue();
        String sessionId = matcher.group(1);
        ScenarioContext.get().setVar("sessionId", sessionId);

        if (complete) {
            boolean completed = false;
            int maxAttempts = 20;
            int attempt = 0;
            while (!completed && attempt < maxAttempts) {
                Response qResp = employeeClient.employeeGet("/api/employee/sessions/" + sessionId + "/questions");
                assertThat(employeeClient.statusCode(qResp))
                        .as("GET questions для сессии %s должен возвращать 200", sessionId)
                        .isEqualTo(200);
                String qBody = employeeClient.body(qResp);
                assertThat(qBody).as("тело GET questions не должно быть пустым").isNotNull().isNotEmpty();
                assertThat(qBody).as("тело GET questions не должно быть 'null'").isNotEqualTo("null");

                String questionAttemptId;
                try {
                    questionAttemptId = JsonPath.read(qBody, "$.questionId");
                } catch (Exception e) {
                    // Сессия завершена: GET questions возвращает {"completed": true}
                    Boolean done = JsonPath.read(qBody, "$.completed");
                    assertThat(done).as("при отсутствии questionId ответ должен содержать completed: %s", qBody).isTrue();
                    break;
                }
                assertThat(questionAttemptId).isNotNull();

                Response aResp = employeeClient.employeePost(
                        "/api/employee/sessions/" + sessionId + "/answers",
                        Map.of("questionAttemptId", questionAttemptId, "finalTranscript", "Тестовый ответ"));
                assertThat(employeeClient.statusCode(aResp))
                        .as("POST answers для сессии %s должен возвращать 200", sessionId)
                        .isEqualTo(200);
                String aBody = employeeClient.body(aResp);
                assertThat(aBody).as("тело POST answers не должно быть пустым").isNotNull().isNotEmpty();
                completed = JsonPath.read(aBody, "$.completed");
                assertThat(completed).as("каждый ответ должен содержать поле completed: %s", aBody).isNotNull();
                attempt++;
            }
            assertThat(completed)
                    .as("сессия должна завершиться в течение %d ответов", maxAttempts)
                    .isTrue();
        }
    }

    private String resolvePath(String path) {
        Matcher matcher = VAR_PATTERN.matcher(path);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = ScenarioContext.get().getVar(varName);
            matcher.appendReplacement(sb, value != null ? value : matcher.group());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
