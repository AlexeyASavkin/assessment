package com.assessment.test.steps;

import com.assessment.test.client.TestHttpClient;
import com.assessment.test.config.TestAdminConfig;
import com.assessment.test.context.ScenarioContext;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.docstring.DocString;
import okhttp3.Response;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class CompetencySteps {

    private static final TestHttpClient client = new TestHttpClient();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)}");

    @Given("администратор авторизован")
    public void adminIsAuthenticated() {
        Map<String, String> params = Map.of("username", TestAdminConfig.username(), "password", TestAdminConfig.password());
        Response response = client.adminLogin(params);
        assertThat(client.statusCode(response)).isEqualTo(200);
        ScenarioContext.get().setLastResponse(client.body(response), client.statusCode(response));
    }

    @When("отправляю POST {string} с JSON")
    public void sendPostWithJson(String path, DocString docString) {
        String resolvedPath = resolvePath(path);
        String resolvedBody = resolveVars(docString.getContent());
        Response response = client.adminPost(resolvedPath, resolvedBody);
        ScenarioContext.get().setLastResponse(client.body(response), client.statusCode(response));
    }

    @When("отправляю PUT {string} с JSON")
    public void sendPutWithJson(String path, DocString docString) {
        String resolvedPath = resolvePath(path);
        String resolvedBody = resolveVars(docString.getContent());
        Response response = client.adminPut(resolvedPath, resolvedBody);
        ScenarioContext.get().setLastResponse(client.body(response), client.statusCode(response));
    }

    @When("отправляю GET {string}")
    public void sendGet(String path) {
        String resolvedPath = resolvePath(path);
        Response response = client.adminGet(resolvedPath);
        ScenarioContext.get().setLastResponse(client.body(response), client.statusCode(response));
    }

    @When("отправляю DELETE {string}")
    public void sendDelete(String path) {
        String resolvedPath = resolvePath(path);
        Response response = client.adminDelete(resolvedPath);
        ScenarioContext.get().setLastResponse(client.body(response), client.statusCode(response));
    }

    @Then("JSON содержит {string} = {string}")
    public void jsonContainsFieldWithValue(String jsonPathExpr, String expectedValue) {
        String body = ScenarioContext.get().lastResponseBody();
        String actual = client.jsonPath(body, jsonPathExpr);
        assertThat(actual).isEqualTo(expectedValue);
    }

    @Then("сохраняю {string} как {string}")
    public void saveFieldAsVariable(String jsonPathExpr, String varName) {
        String body = ScenarioContext.get().lastResponseBody();
        String value = client.jsonPath(body, jsonPathExpr);
        ScenarioContext.get().setVar(varName, value);
    }

    @Then("существует {string}")
    public void variableExists(String varName) {
        String value = ScenarioContext.get().getVar(varName);
        assertThat(value).isNotNull();
    }

    @Then("JSON содержит хотя бы один элемент с {string} = {string}")
    public void jsonArrayContainsElementWithField(String jsonPathExpr, String expectedValue) {
        String body = ScenarioContext.get().lastResponseBody();
        List<Map<String, Object>> items = JsonPath.read(body, "$");
        String field = jsonPathExpr.replaceAll("^\\$\\.\\[\\?\\(@\\.", "").replaceAll("==.*\\]$", "");
        boolean found = items.stream()
                .anyMatch(item -> String.valueOf(item.get(field)).equals(expectedValue));
        assertThat(found).isTrue();
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
