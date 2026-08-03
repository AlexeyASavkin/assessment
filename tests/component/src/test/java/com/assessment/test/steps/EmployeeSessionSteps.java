package com.assessment.test.steps;

import com.assessment.test.client.TestHttpClient;
import com.assessment.test.config.TestAdminConfig;
import com.assessment.test.context.ScenarioContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.docstring.DocString;
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

public class EmployeeSessionSteps {

    private static final TestHttpClient adminClient = new TestHttpClient();
    private static final TestHttpClient employeeClient = new TestHttpClient();
    private static final TestHttpClient secondEmployeeClient = new TestHttpClient();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)}");
    private static final Pattern SESSION_UUID_PATTERN = Pattern.compile("/session/([0-9a-fA-F-]+)");
    private Response lastEmployeeResponse;

    @Given("администратор авторизован для сессии")
    public void adminAuthForSession() {
        Map<String, String> params = Map.of("username", TestAdminConfig.username(), "password", TestAdminConfig.password());
        Response response = adminClient.postForm("/api/admin/login", params);
        assertThat(adminClient.statusCode(response)).isEqualTo(200);
        ScenarioContext.get().setLastResponse(adminClient.body(response), adminClient.statusCode(response));
    }

    @Given("существует компетенция с разделом и темой и сгенерированными вопросами")
    public void createCompetencyWithSectionTopicAndQuestions() {
        createCompetencyWithQuestions(1);
    }

    @Given("существует компетенция с разделом и темой и {int} сгенерированными вопросами")
    public void createCompetencyWithSectionTopicAndNQuestions(int count) {
        createCompetencyWithQuestions(count);
    }

    private void createCompetencyWithQuestions(int count) {
        String uniqueName = "Java Session " + UUID.randomUUID().toString().substring(0, 8);
        Response compResp = adminClient.adminPost("/api/admin/competencies",
                Map.of("name", uniqueName, "description", "Оценка знаний Java"));
        assertThat(adminClient.statusCode(compResp)).isEqualTo(201);
        String compBody = adminClient.body(compResp);
        String competencyId = JsonPath.read(compBody, "$.id");
        ScenarioContext.get().setVar("competencyId", competencyId);

        Response secResp = adminClient.adminPost("/api/admin/competencies/" + competencyId + "/sections",
                Map.of("name", "Java Core"));
        assertThat(adminClient.statusCode(secResp)).isEqualTo(201);
        String secBody = adminClient.body(secResp);
        String sectionId = JsonPath.read(secBody, "$.id");
        ScenarioContext.get().setVar("sectionId", sectionId);

        Response topicResp = adminClient.adminPost("/api/admin/sections/" + sectionId + "/topics",
                Map.of("name", "Stream API"));
        assertThat(adminClient.statusCode(topicResp)).isEqualTo(201);
        String topicBody = adminClient.body(topicResp);
        String topicId = JsonPath.read(topicBody, "$.id");
        ScenarioContext.get().setVar("topicId", topicId);

        Response genResp = adminClient.adminPost(
                "/api/admin/competencies/" + competencyId + "/questions/generate",
                Map.of("count", count, "difficulty", "MIDDLE"));
        assertThat(adminClient.statusCode(genResp)).isIn(200, 202);
        ScenarioContext.get().setLastResponse(adminClient.body(genResp), adminClient.statusCode(genResp));
    }

    @Given("существует сотрудник с пригласительной ссылкой")
    public void createEmployeeWithInvite() {
        String competencyId = ScenarioContext.get().getVar("competencyId");
        Response empResp = adminClient.adminPost("/api/admin/employees",
                Map.of("fullName", "Тест Сотрудник", "email", "test@test.com", "competencyId", competencyId));
        assertThat(adminClient.statusCode(empResp)).isEqualTo(201);
        String empBody = adminClient.body(empResp);
        String employeeId = JsonPath.read(empBody, "$.id");
        ScenarioContext.get().setVar("employeeId", employeeId);

        Response tokenResp = adminClient.adminPost("/api/admin/employees/" + employeeId + "/invite", "");
        assertThat(adminClient.statusCode(tokenResp)).isEqualTo(200);
        String tokenBody = adminClient.body(tokenResp);
        String tokenPath = tokenBody.replace("\"", "").trim();
        String token = tokenPath.substring(tokenPath.lastIndexOf('/') + 1);
        ScenarioContext.get().setVar("inviteToken", token);
    }

    @Given("сотрудник открыл пригласительную ссылку и получил сессию")
    public void employeeOpensInvite() {
        String token = ScenarioContext.get().getVar("inviteToken");
        Response response = employeeClient.openInvite(token);
        int status = employeeClient.statusCode(response);
        assertThat(status).isIn(200, 302, 303);

        String location = employeeClient.header(response, "Location");
        if (location != null && !location.isEmpty()) {
            Matcher matcher = SESSION_UUID_PATTERN.matcher(location);
            assertThat(matcher.find()).isTrue();
            ScenarioContext.get().setVar("sessionId", matcher.group(1));
        }
        ScenarioContext.get().setLastResponse(employeeClient.body(response), status);
    }

    @Given("сотрудник повторно открывает пригласительную ссылку")
    public void employeeReopensInvite() {
        String token = ScenarioContext.get().getVar("inviteToken");
        Response response = employeeClient.openInvite(token);
        ScenarioContext.get().setLastResponse(employeeClient.body(response), employeeClient.statusCode(response));
        String location = employeeClient.header(response, "Location");
        assertThat(location).as("повторное открытие ссылки должно вести на сессию").contains("/session/");
        String sessionId = ScenarioContext.get().getVar("sessionId");
        assertThat(location).as("повторное открытие ссылки должно возвращать ту же сессию")
                .contains("/session/" + sessionId);
    }

    @Given("сотрудник открывает пригласительную ссылку с невалидным токеном")
    public void employeeOpensInviteWithInvalidToken() {
        Response response = employeeClient.openInvite("invalid-token-123");
        ScenarioContext.get().setLastResponse(employeeClient.body(response), employeeClient.statusCode(response));
    }

    @Given("существует второй сотрудник с пригласительной ссылкой")
    public void createSecondEmployeeWithInvite() {
        String competencyId = ScenarioContext.get().getVar("competencyId");
        Response empResp = adminClient.adminPost("/api/admin/employees",
                Map.of("fullName", "Второй Сотрудник", "email", "second@test.com", "competencyId", competencyId));
        assertThat(adminClient.statusCode(empResp)).isEqualTo(201);
        String empBody = adminClient.body(empResp);
        String employeeId = JsonPath.read(empBody, "$.id");
        ScenarioContext.get().setVar("secondEmployeeId", employeeId);

        Response tokenResp = adminClient.adminPost("/api/admin/employees/" + employeeId + "/invite", "");
        assertThat(adminClient.statusCode(tokenResp)).isEqualTo(200);
        String tokenBody = adminClient.body(tokenResp);
        String tokenPath = tokenBody.replace("\"", "").trim();
        String token = tokenPath.substring(tokenPath.lastIndexOf('/') + 1);
        ScenarioContext.get().setVar("inviteToken2", token);
    }

    @Given("второй сотрудник открыл пригласительную ссылку")
    public void secondEmployeeOpensInvite() {
        String token = ScenarioContext.get().getVar("inviteToken2");
        Response response = secondEmployeeClient.openInvite(token);
        int status = secondEmployeeClient.statusCode(response);
        assertThat(status).isIn(200, 302, 303);
        String location = secondEmployeeClient.header(response, "Location");
        assertThat(location).contains("/session/");
        Matcher matcher = SESSION_UUID_PATTERN.matcher(location);
        assertThat(matcher.find()).isTrue();
        ScenarioContext.get().setVar("secondSessionId", matcher.group(1));
        ScenarioContext.get().setLastResponse(secondEmployeeClient.body(response), status);
    }

    @When("отправляю GET {string} как второй сотрудник для сессии")
    public void secondEmployeeGetForSession(String path) {
        String resolvedPath = resolvePath(path);
        lastEmployeeResponse = secondEmployeeClient.employeeGet(resolvedPath);
        ScenarioContext.get().setLastResponse(
                secondEmployeeClient.body(lastEmployeeResponse),
                secondEmployeeClient.statusCode(lastEmployeeResponse));
    }

    @When("отправляю GET {string} как сотрудник для сессии")
    public void employeeGetForSession(String path) {
        String resolvedPath = resolvePath(path);
        lastEmployeeResponse = employeeClient.employeeGet(resolvedPath);
        ScenarioContext.get().setLastResponse(
                employeeClient.body(lastEmployeeResponse),
                employeeClient.statusCode(lastEmployeeResponse));
    }

    @When("отправляю POST {string} с JSON как сотрудник для ответа")
    public void employeePostAnswer(String path, DocString docString) {
        String resolvedPath = resolvePath(path);
        // Resolve {varName} placeholders inside JSON body too
        String resolvedBody = resolveVars(docString.getContent());
        lastEmployeeResponse = employeeClient.employeePost(resolvedPath, resolvedBody);
        ScenarioContext.get().setLastResponse(
                employeeClient.body(lastEmployeeResponse),
                employeeClient.statusCode(lastEmployeeResponse));
    }

    @Then("статус ответа сессии {int}")
    public void sessionStatusCodeIs(int expected) {
        assertThat(ScenarioContext.get().lastStatusCode()).isEqualTo(expected);
    }

    @Then("JSON сессии содержит {string}")
    public void sessionJsonContains(String jsonPathExpr) {
        String body = ScenarioContext.get().lastResponseBody();
        Object value = JsonPath.read(body, jsonPathExpr);
        assertThat(value).isNotNull();
    }

    @Then("сохраняю из JSON сессии {string} как {string}")
    public void saveFromSessionJson(String jsonPathExpr, String varName) {
        String body = ScenarioContext.get().lastResponseBody();
        String value = JsonPath.read(body, jsonPathExpr).toString();
        ScenarioContext.get().setVar(varName, value);
    }

    @Then("сессия завершена \\(предыдущий ответ вернул completed: true)")
    public void sessionIsCompleted() {
        String body = ScenarioContext.get().lastResponseBody();
        Boolean completed = JsonPath.read(body, "$.completed");
        assertThat(completed).isTrue();
    }

    @Then("JSON сессии содержит {string} = {string}")
    public void sessionJsonFieldEquals(String jsonPathExpr, String expectedValue) {
        String body = ScenarioContext.get().lastResponseBody();
        String actual = JsonPath.read(body, jsonPathExpr).toString();
        assertThat(actual).isEqualTo(expectedValue);
    }

    private String resolvePath(String path) {
        return resolveVars(path);
    }

    private String resolveVars(String text) {
        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = ScenarioContext.get().getVar(varName);
            matcher.appendReplacement(sb, value != null ? Matcher.quoteReplacement(value) : matcher.group());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
