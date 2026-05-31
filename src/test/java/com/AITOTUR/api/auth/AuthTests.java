package com.academyai.api.auth;

import com.academyai.utils.BaseApiTest;
import com.academyai.utils.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("AcademyAI Platform")
@Feature("Authentication")
public class AuthTests extends BaseApiTest {

    // ── Register ───────────────────────────────────────────

    @Test(priority = 1, groups = {"smoke", "auth"})
    @Story("Student Registration")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Register a new student account with valid data and verify 201 Created")
    public void testRegisterNewStudent_ValidData_Returns201() {
        String email    = TestDataFactory.randomStudentEmail();
        String password = TestDataFactory.randomPassword();

        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{"
                    + "\"name\": \"" + TestDataFactory.randomName() + "\","
                    + "\"email\": \"" + email + "\","
                    + "\"password\": \"" + password + "\","
                    + "\"university_id\": \"" + config.testUniversityId() + "\","
                    + "\"faculty_id\": \"" + config.testFacultyId() + "\","
                    + "\"academic_year\": " + TestDataFactory.randomAcademicYear() + ","
                    + "\"student_id\": \"" + TestDataFactory.randomUniversityId() + "\""
                    + "}")
                .post("/api/v1/auth/register");

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.jsonPath().getString("data.token")).isNotBlank();
        assertThat(response.jsonPath().getString("data.user.email")).isEqualTo(email);
        assertThat(response.time()).isLessThan(config.perfAuthMaxResponseMs());

        log.info("Register OK – email: {}", email);
    }

    @Test(priority = 2, groups = {"auth"})
    @Story("Student Registration")
    @Severity(SeverityLevel.NORMAL)
    @Description("Register with duplicate email must return 409 Conflict")
    public void testRegisterDuplicateEmail_Returns409() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{"
                    + "\"name\": \"Test Student\","
                    + "\"email\": \"" + config.studentEmail() + "\","
                    + "\"password\": \"" + TestDataFactory.randomPassword() + "\","
                    + "\"university_id\": \"" + config.testUniversityId() + "\","
                    + "\"faculty_id\": \"" + config.testFacultyId() + "\","
                    + "\"academic_year\": 2,"
                    + "\"student_id\": \"STU-999999\""
                    + "}")
                .post("/api/v1/auth/register");

        assertThat(response.getStatusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("error")).containsIgnoringCase("already");
    }

    @Test(priority = 3, groups = {"auth"})
    @Story("Student Registration")
    @Severity(SeverityLevel.NORMAL)
    @Description("Register with missing required fields must return 400 Bad Request")
    public void testRegisterMissingFields_Returns400() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{\"email\": \"missing@academyai.edu.eg\"}")
                .post("/api/v1/auth/register");

        assertThat(response.getStatusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("message")).isNotBlank();
    }

    @Test(priority = 4, groups = {"auth"})
    @Story("Student Registration")
    @Severity(SeverityLevel.MINOR)
    @Description("Register with invalid email format must return 422 Unprocessable Entity")
    public void testRegisterInvalidEmailFormat_Returns422() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{"
                    + "\"name\": \"Bad Email\","
                    + "\"email\": \"not-an-email\","
                    + "\"password\": \"Test@1234\","
                    + "\"university_id\": \"" + config.testUniversityId() + "\","
                    + "\"faculty_id\": \"" + config.testFacultyId() + "\","
                    + "\"academic_year\": 1,"
                    + "\"student_id\": \"STU-123456\""
                    + "}")
                .post("/api/v1/auth/register");

        assertThat(response.getStatusCode()).isIn(400, 422);
    }

    // ── Login ──────────────────────────────────────────────

    @Test(priority = 5, groups = {"smoke", "auth"}, dependsOnMethods = {})
    @Story("Student Login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Login with valid credentials and capture JWT token for downstream tests")
    public void testLogin_ValidCredentials_ReturnsToken() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{"
                    + "\"email\": \"" + config.studentEmail() + "\","
                    + "\"password\": \"" + config.studentPassword() + "\""
                    + "}")
                .post("/api/v1/auth/login");

        assertThat(response.getStatusCode()).isEqualTo(200);

        String token = response.jsonPath().getString("data.token");
        assertThat(token).isNotBlank();

        // Store globally for other test classes
        bearerToken  = token;
        studentToken = token;

        assertThat(response.jsonPath().getString("data.user.email"))
                .isEqualTo(config.studentEmail());
        assertThat(response.time()).isLessThan(config.perfAuthMaxResponseMs());

        log.info("Login OK – token captured (length: {})", token.length());
    }

    @Test(priority = 6, groups = {"auth"})
    @Story("Student Login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Login with wrong password must return 401 Unauthorized")
    public void testLogin_WrongPassword_Returns401() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{"
                    + "\"email\": \"" + config.studentEmail() + "\","
                    + "\"password\": \"WrongPass@9999\""
                    + "}")
                .post("/api/v1/auth/login");

        assertThat(response.getStatusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("error"))
                .containsIgnoringCase("invalid");
    }

    @Test(priority = 7, groups = {"auth"})
    @Story("Student Login")
    @Severity(SeverityLevel.NORMAL)
    @Description("Login with non-existent email must return 404 Not Found")
    public void testLogin_NonExistentEmail_Returns404() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{"
                    + "\"email\": \"ghost.user." + System.currentTimeMillis() + "@academyai.edu.eg\","
                    + "\"password\": \"Test@1234\""
                    + "}")
                .post("/api/v1/auth/login");

        assertThat(response.getStatusCode()).isIn(401, 404);
    }

    @Test(priority = 8, groups = {"auth"})
    @Story("Student Login")
    @Severity(SeverityLevel.MINOR)
    @Description("Login with empty body must return 400 Bad Request")
    public void testLogin_EmptyBody_Returns400() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .body("{}")
                .post("/api/v1/auth/login");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    // ── Profile ────────────────────────────────────────────

    @Test(priority = 9, groups = {"smoke", "auth"})
    @Story("Student Profile")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET current student profile with valid token returns 200 and correct data")
    public void testGetProfile_ValidToken_Returns200() {
        Response response = authedSpec()
                .get("/api/v1/auth/profile");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.email")).isNotBlank();
        assertThat(response.jsonPath().getString("data.name")).isNotBlank();
        assertThat(response.time()).isLessThan(config.perfAuthMaxResponseMs());
    }

    @Test(priority = 10, groups = {"auth"})
    @Story("Student Profile")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET profile without token must return 401 Unauthorized")
    public void testGetProfile_NoToken_Returns401() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .get("/api/v1/auth/profile");

        assertThat(response.getStatusCode()).isEqualTo(401);
    }

    @Test(priority = 11, groups = {"auth"})
    @Story("Student Profile")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET profile with expired/invalid token must return 401")
    public void testGetProfile_InvalidToken_Returns401() {
        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("Authorization", "Bearer invalid.token.xyz")
                .get("/api/v1/auth/profile");

        assertThat(response.getStatusCode()).isEqualTo(401);
    }

    @Test(priority = 12, groups = {"auth"})
    @Story("Student Profile")
    @Severity(SeverityLevel.NORMAL)
    @Description("Update student profile with valid data returns 200")
    public void testUpdateProfile_ValidData_Returns200() {
        String newName = TestDataFactory.randomName();

        Response response = authedSpec()
                .body("{\"name\": \"" + newName + "\"}")
                .put("/api/v1/auth/profile");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.name")).isEqualTo(newName);
    }

    // ── Token Refresh ──────────────────────────────────────

    @Test(priority = 13, groups = {"auth"})
    @Story("Token Management")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Refresh token endpoint returns new valid JWT")
    public void testRefreshToken_ValidRefreshToken_Returns200() {
        // First login to get refresh token
        Response loginResp = RestAssured.given()
                .spec(requestSpec)
                .body("{"
                    + "\"email\": \"" + config.studentEmail() + "\","
                    + "\"password\": \"" + config.studentPassword() + "\""
                    + "}")
                .post("/api/v1/auth/login");

        String refreshToken = loginResp.jsonPath().getString("data.refresh_token");

        Response refreshResp = RestAssured.given()
                .spec(requestSpec)
                .body("{\"refresh_token\": \"" + refreshToken + "\"}")
                .post("/api/v1/auth/refresh");

        assertThat(refreshResp.getStatusCode()).isEqualTo(200);
        assertThat(refreshResp.jsonPath().getString("data.token")).isNotBlank();
    }

    // ── Logout ─────────────────────────────────────────────

    @Test(priority = 14, groups = {"auth"}, alwaysRun = true)
    @Story("Logout")
    @Severity(SeverityLevel.NORMAL)
    @Description("Logout invalidates token and returns 200")
    public void testLogout_ValidToken_Returns200() {
        Response response = authedSpec()
                .post("/api/v1/auth/logout");

        assertThat(response.getStatusCode()).isEqualTo(200);

        // Verify token is now invalid
        Response profileAfterLogout = authedSpec()
                .get("/api/v1/auth/profile");
        assertThat(profileAfterLogout.getStatusCode()).isEqualTo(401);
    }
}
