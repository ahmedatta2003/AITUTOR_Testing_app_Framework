package com.academyai.utils;

import com.academyai.config.ConfigManager;
import com.academyai.config.FrameworkConfig;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

import static org.hamcrest.Matchers.lessThan;

/**
 * Base class for all API tests.
 * Sets up RestAssured request/response specs and Allure filter.
 */
public abstract class BaseApiTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseApiTest.class);
    protected static final FrameworkConfig config = ConfigManager.getConfig();

    protected static RequestSpecification requestSpec;
    protected static RequestSpecification aiRequestSpec;
    protected static ResponseSpecification responseSpec;

    // Shared token – populated by AuthTests and reused by others
    protected static String bearerToken;
    protected static String studentToken;

    @BeforeClass(alwaysRun = true)
    public void setupSpec() {

        // ── Main API spec ──────────────────────────────────
        requestSpec = new RequestSpecBuilder()
                .setBaseUri(config.baseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .log(LogDetail.ALL)
                .build();

        // ── AI API spec (longer timeout) ───────────────────
        aiRequestSpec = new RequestSpecBuilder()
                .setBaseUri(config.aiBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new AllureRestAssured())
                .log(LogDetail.ALL)
                .build();

        // ── Response spec (basic) ──────────────────────────
        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .build();

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        log.info("BaseApiTest setup complete. Base URL: {}", config.baseUrl());
    }

    /**
     * Returns a RequestSpecification with Authorization header injected.
     */
    protected RequestSpecification authedSpec() {
        return RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + bearerToken);
    }

    protected RequestSpecification authedAiSpec() {
        return RestAssured.given()
                .spec(aiRequestSpec)
                .header("Authorization", "Bearer " + bearerToken);
    }

    protected RequestSpecification studentAuthedSpec() {
        return RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + studentToken);
    }
}
