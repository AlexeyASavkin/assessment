package com.assessment.test;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FEATURES_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber test runner для black-box тестирования сервиса.
 * <p>
 * Запускает Gherkin-сценарии из {@code features/} с шагами в пакете {@code steps}.
 * Использует JUnit Platform Suite для интеграции Cucumber с Gradle test-задачей.
 * <p>
 * Запуск: {@code ./gradlew -p tests/component test}
 * Предусловие: запущен PostgreSQL и бэкенд на порту 8081 (см. run-bdd-tests.bat)
 */
@Suite
@IncludeEngines("cucumber")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.assessment.test.steps")
@ConfigurationParameter(key = FEATURES_PROPERTY_NAME, value = "src/test/resources/features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,json:build/reports/cucumber.json,html:build/reports/cucumber-reports.html,io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm")
public class CucumberTestRunner {
    // Runner class — no additional code needed
}
