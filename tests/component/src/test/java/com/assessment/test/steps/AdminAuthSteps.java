package com.assessment.test.steps;

import com.assessment.test.client.TestHttpClient;
import com.assessment.test.context.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import okhttp3.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminAuthSteps {

    private static final TestHttpClient client = new TestHttpClient();
    private Response lastResponse;

    @When("отправляю POST {string} с form-параметрами")
    public void sendPostWithFormParams(String path, DataTable table) {
        Map<String, String> params = new LinkedHashMap<>();
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            params.put(row.get("param"), row.get("value"));
        }
        if ("/api/admin/login".equals(path)) {
            // Deferred-token CSRF bootstrap (T6/T8): первый POST без XSRF-TOKEN -> 302 + Set-Cookie,
            // повторный POST с заголовком X-XSRF-TOKEN -> 200 (успех) / 401 (неверный пароль).
            lastResponse = client.adminLogin(params);
        } else {
            lastResponse = client.postForm(path, params);
        }
        ScenarioContext.get().setLastResponse(client.body(lastResponse), client.statusCode(lastResponse));
    }

    @Then("статус ответа {int}")
    public void statusCodeIs(int expected) {
        assertThat(ScenarioContext.get().lastStatusCode()).isEqualTo(expected);
    }

    @Then("cookie {string} установлена")
    public void cookieIsSet(String cookieName) {
        assertThat(lastResponse).isNotNull();
        // Успешный login шлёт НЕСКОЛЬКО Set-Cookie (JSESSIONID + старый XSRF-TOKEN
        // очищается + новый XSRF-TOKEN). OkHttp header() возвращает только последний,
        // поэтому проверяем все значения заголовка.
        List<String> setCookies = client.headers(lastResponse, "Set-Cookie");
        assertThat(setCookies).anyMatch(sc -> sc.contains(cookieName));
    }

    @Given("предыдущий запрос был с неверным паролем")
    public void previousRequestWithWrongPassword() {
    }
}
