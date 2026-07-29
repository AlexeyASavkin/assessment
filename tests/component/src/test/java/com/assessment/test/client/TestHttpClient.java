package com.assessment.test.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.*;
import java.util.function.Consumer;

/**
 * HTTP-клиент для BDD-тестов чёрного ящика.
 * <p>
 * Управляет сессиями (admin + employee) через куки, делает JSON-запросы и ответы.
 * Все методы бросают RuntimeException при сетевых ошибках.
 */
public class TestHttpClient {

    private static final String BASE_URL = "http://localhost:8081";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OkHttpClient adminClient;
    private final OkHttpClient employeeClient;
    private final OkHttpClient noRedirectClient;

    public TestHttpClient() {
        // Shared cookie jar so adminClient, employeeClient, and noRedirectClient
        // all share session cookies within the same TestHttpClient instance.
        CookieJar sharedJar = new SessionCookieJar();
        this.adminClient = buildClient(true, sharedJar);
        this.employeeClient = buildClient(true, sharedJar);
        this.noRedirectClient = buildClient(false, sharedJar);
    }

    private static OkHttpClient buildClient(boolean followRedirects, CookieJar cookieJar) {
        return new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .followRedirects(followRedirects)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    // ---- Admin API ----

    public Response postForm(String path, Map<String, String> formParams) {
        FormBody.Builder form = new FormBody.Builder();
        formParams.forEach(form::add);
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .post(form.build())
                .build();
        return execute(adminClient, request);
    }

    public Response adminPost(String path, Object body) {
        return adminJson("POST", path, body);
    }

    public Response adminPut(String path, Object body) {
        return adminJson("PUT", path, body);
    }

    public Response adminGet(String path) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .get()
                .build();
        return execute(adminClient, request);
    }

    public Response adminDelete(String path) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .delete()
                .build();
        return execute(adminClient, request);
    }

    private Response adminJson(String method, String path, Object body) {
        try {
            String json = body instanceof String ? (String) body : MAPPER.writeValueAsString(body);
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(json, JSON);

            Request.Builder builder = new Request.Builder()
                    .url(BASE_URL + path);

            switch (method) {
                case "POST" -> builder.post(requestBody);
                case "PUT" -> builder.put(requestBody);
                default -> throw new IllegalArgumentException("Unsupported method: " + method);
            }

            return execute(adminClient, builder.build());
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    // ---- Employee API ----

    public Response employeeGet(String path) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .get()
                .build();
        return execute(employeeClient, request);
    }

    public Response employeePost(String path, Object body) {
        try {
            String json = body instanceof String ? (String) body : MAPPER.writeValueAsString(body);
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody requestBody = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(BASE_URL + path)
                    .post(requestBody)
                    .build();
            return execute(employeeClient, request);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    // ---- Employee auth (invite) ----

    public Response openInvite(String token) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/employee/invite/" + token)
                .build();
        return execute(noRedirectClient, request);
    }

    // ---- Shared ----

    public Response adminGetNoRedirect(String path) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .get()
                .build();
        return execute(noRedirectClient, request);
    }

    public Response rawGet(String path) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .get()
                .build();
        return execute(noRedirectClient, request); // no auth, no redirects
    }

    public int statusCode(Response response) {
        return response.code();
    }

    public String body(Response response) {
        try {
            return response.body() != null ? response.body().string() : "";
        } catch (IOException e) {
            throw new RuntimeException("Failed to read response body", e);
        }
    }

    public <T> T parseJson(Response response, Class<T> clazz) {
        try {
            return MAPPER.readValue(body(response), clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }

    public <T> T parseJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public String jsonPath(String json, String path) {
        try {
            return com.jayway.jsonpath.JsonPath.read(json, path).toString();
        } catch (Exception e) {
            throw new RuntimeException("JSONPath '" + path + "' failed: " + e.getMessage());
        }
    }

    public String header(Response response, String name) {
        String val = response.header(name);
        return val != null ? val : "";
    }

    // ---- Internal ----

    private static Response execute(OkHttpClient client, Request request) {
        try {
            return client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException("HTTP request failed: " + request.method() + " " + request.url(), e);
        }
    }

    /**
     * Хранит куки в памяти — поддерживает несколько параллельных сессий.
     * Заменяет существующие куки с тем же именем+доменом+путем, предотвращая
     * накопление устаревших кук между сценариями.
     */
    private static class SessionCookieJar implements CookieJar {
        private final List<Cookie> cookies = new ArrayList<>();

        @Override
        public void saveFromResponse(@NotNull HttpUrl url, @NotNull List<Cookie> newCookies) {
            for (Cookie newCookie : newCookies) {
                // Remove existing cookie with same name+domain+path before adding
                cookies.removeIf(existing ->
                        existing.name().equals(newCookie.name())
                                && existing.domain().equals(newCookie.domain())
                                && existing.path().equals(newCookie.path()));
                cookies.add(newCookie);
            }
        }

        @Override
        @NotNull
        public List<Cookie> loadForRequest(@NotNull HttpUrl url) {
            List<Cookie> valid = new ArrayList<>();
            for (Cookie cookie : cookies) {
                if (cookie.matches(url) && cookie.expiresAt() > System.currentTimeMillis()) {
                    valid.add(cookie);
                }
            }
            return valid;
        }
    }
}
