package com.assessment.test.steps;

import com.assessment.test.client.TestHttpClient;
import com.assessment.test.context.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.docstring.DocString;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import okhttp3.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class EmployeeSteps {

    private static final TestHttpClient client = new TestHttpClient();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)}");
    private static final Pattern SESSION_UUID_PATTERN = Pattern.compile("/session/([0-9a-fA-F-]+)");
    private Response lastResponse;

    @When("отправляю POST {string} с JSON как сотрудник")
    public void employeePostWithJson(String path, DocString docString) {
        String resolvedPath = resolvePath(path);
        lastResponse = client.employeePost(resolvedPath, docString.getContent());
        ScenarioContext.get().setLastResponse(client.body(lastResponse), client.statusCode(lastResponse));
    }

    @When("отправляю POST {string} с form-параметрами как сотрудник")
    public void employeePostWithFormParams(String path, DataTable table) {
        String resolvedPath = resolvePath(path);
        Map<String, String> params = new LinkedHashMap<>();
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            params.put(row.get("param"), row.get("value"));
        }
        lastResponse = client.postForm(resolvedPath, params);
        ScenarioContext.get().setLastResponse(client.body(lastResponse), client.statusCode(lastResponse));
    }

    @When("отправляю GET {string} без авторизации")
    public void getWithoutAuth(String path) {
        String resolvedPath = resolvePath(path);
        lastResponse = client.rawGet(resolvedPath);
        ScenarioContext.get().setLastResponse(client.body(lastResponse), client.statusCode(lastResponse));
    }

    @When("сотрудник открывает пригласительную ссылку")
    public void employeeOpensInviteLink() {
        String inviteUrl = ScenarioContext.get().getVar("inviteUrl");
        assertThat(inviteUrl).isNotNull();
        String token = inviteUrl.substring(inviteUrl.lastIndexOf('/') + 1);
        lastResponse = client.openInvite(token);
        ScenarioContext.get().setLastResponse(client.body(lastResponse), client.statusCode(lastResponse));
    }

    @Then("статус ответа как сотрудник {int}")
    public void employeeStatusCodeIs(int expected) {
        assertThat(ScenarioContext.get().lastStatusCode()).isEqualTo(expected);
    }

    @Then("тело ответа содержит {string}")
    public void responseBodyContains(String substring) {
        assertThat(ScenarioContext.get().lastResponseBody()).contains(substring);
    }

    @Then("сохраняю тело ответа как {string}")
    public void saveResponseBodyAs(String varName) {
        ScenarioContext.get().setVar(varName, ScenarioContext.get().lastResponseBody());
    }

    @Then("заголовок {string} содержит {string}")
    public void headerContains(String headerName, String expected) {
        assertThat(lastResponse).isNotNull();
        String value = client.header(lastResponse, headerName);
        assertThat(value).contains(expected);
    }

    @Then("cookie как сотрудник {string} установлена")
    public void employeeCookieIsSet(String cookieName) {
        assertThat(lastResponse).isNotNull();
        String setCookie = client.header(lastResponse, "Set-Cookie");
        assertThat(setCookie).contains(cookieName);
    }

    @Then("сохраняю sessionId из Location")
    public void saveSessionIdFromLocation() {
        assertThat(lastResponse).isNotNull();
        String location = client.header(lastResponse, "Location");
        Matcher matcher = SESSION_UUID_PATTERN.matcher(location);
        assertThat(matcher.find()).isTrue();
        ScenarioContext.get().setVar("sessionId", matcher.group(1));
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
