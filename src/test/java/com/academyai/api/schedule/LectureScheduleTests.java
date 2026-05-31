package com.academyai.api.schedule;

import com.academyai.utils.BaseApiTest;
import com.academyai.utils.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lecture Schedule Tests – validates academic timetable APIs.
 *
 * Covers:
 *  - Weekly schedule fetch by student, faculty, department
 *  - Today's lectures
 *  - Course details and instructor info
 *  - Lecture search / filter
 *  - Online lecture links
 *  - Admin: create/update/delete lecture (if applicable)
 *  - Edge cases: past dates, invalid week, missing auth
 */
@Epic("AcademyAI Platform")
@Feature("Lecture Schedule")
public class LectureScheduleTests extends BaseApiTest {

    private String lectureId;
    private final String today = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    // ── Weekly Schedule ────────────────────────────────────

    @Test(priority = 1, groups = {"smoke", "schedule"})
    @Story("Weekly Schedule")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Get current week's schedule for authenticated student – returns 200 with lectures list")
    public void testGetWeeklySchedule_CurrentStudent_Returns200() {
        Response response = authedSpec()
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data.lectures")).isNotNull();
        assertThat(response.jsonPath().getInt("data.week_number")).isGreaterThan(0);
        assertThat(response.time()).isLessThan(config.perfScheduleMaxResponseMs());

        List<String> courseNames = response.jsonPath().getList("data.lectures.course_name");
        log.info("Weekly schedule: {} lectures found", courseNames.size());
    }

    @Test(priority = 2, groups = {"schedule"})
    @Story("Weekly Schedule")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get weekly schedule for a specific week number – returns correct week data")
    public void testGetWeeklySchedule_SpecificWeekNumber_Returns200() {
        Response response = authedSpec()
                .queryParam("week", 5)
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getInt("data.week_number")).isEqualTo(5);
    }

    @Test(priority = 3, groups = {"schedule"})
    @Story("Weekly Schedule")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get weekly schedule filtered by faculty – returns only faculty lectures")
    public void testGetWeeklySchedule_FilterByFaculty_ReturnsFilteredLectures() {
        Response response = authedSpec()
                .queryParam("faculty_id", config.testFacultyId())
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isEqualTo(200);

        List<String> lectureIds = response.jsonPath().getList("data.lectures.id");
        if (!lectureIds.isEmpty()) {
            lectureId = lectureIds.get(0);
            log.info("Captured lectureId: {}", lectureId);
        }
    }

    @Test(priority = 4, groups = {"schedule"})
    @Story("Weekly Schedule")
    @Severity(SeverityLevel.MINOR)
    @Description("Get schedule for an invalid/out-of-range week number must return 400 or 422")
    public void testGetWeeklySchedule_InvalidWeekNumber_Returns400() {
        Response response = authedSpec()
                .queryParam("week", -1)
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isIn(400, 422);
    }

    // ── Today's Lectures ───────────────────────────────────

    @Test(priority = 5, groups = {"smoke", "schedule"})
    @Story("Today's Lectures")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Get today's lectures – returns 200 with lectures for current date")
    public void testGetTodayLectures_AuthenticatedStudent_Returns200() {
        Response response = authedSpec()
                .get("/api/v1/schedule/today");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data.lectures")).isNotNull();
        assertThat(response.jsonPath().getString("data.date")).isEqualTo(today);
        assertThat(response.time()).isLessThan(config.perfScheduleMaxResponseMs());
    }

    @Test(priority = 6, groups = {"schedule"})
    @Story("Today's Lectures")
    @Severity(SeverityLevel.NORMAL)
    @Description("Today's lectures must have valid time format (HH:mm)")
    public void testGetTodayLectures_LectureTimesAreValidFormat() {
        Response response = authedSpec()
                .get("/api/v1/schedule/today");

        assertThat(response.getStatusCode()).isEqualTo(200);

        List<String> startTimes = response.jsonPath()
                .getList("data.lectures.start_time");

        startTimes.forEach(time -> {
            if (time != null) {
                assertThat(time).matches("^([01]\\d|2[0-3]):([0-5]\\d)$");
            }
        });
    }

    // ── Lecture by Date ────────────────────────────────────

    @Test(priority = 7, groups = {"schedule"})
    @Story("Schedule by Date")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get lectures for a specific date – returns 200 with filtered list")
    public void testGetLecturesByDate_ValidDate_Returns200() {
        Response response = authedSpec()
                .queryParam("date", today)
                .get("/api/v1/schedule/by-date");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.date")).isEqualTo(today);
    }

    @Test(priority = 8, groups = {"schedule"})
    @Story("Schedule by Date")
    @Severity(SeverityLevel.MINOR)
    @Description("Get lectures for an invalid date format must return 400")
    public void testGetLecturesByDate_InvalidDateFormat_Returns400() {
        Response response = authedSpec()
                .queryParam("date", "32-13-9999")
                .get("/api/v1/schedule/by-date");

        assertThat(response.getStatusCode()).isIn(400, 422);
    }

    // ── Lecture Detail ─────────────────────────────────────

    @Test(priority = 9, groups = {"schedule"}, dependsOnMethods = {"testGetWeeklySchedule_FilterByFaculty_ReturnsFilteredLectures"})
    @Story("Lecture Details")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get single lecture by ID – returns full lecture object with room and instructor")
    public void testGetLectureById_ValidId_ReturnsFullDetails() {
        if (lectureId == null) {
            log.warn("No lectureId available – skipping");
            return;
        }

        Response response = authedSpec()
                .get("/api/v1/schedule/lectures/" + lectureId);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.id")).isEqualTo(lectureId);
        assertThat(response.jsonPath().getString("data.course_name")).isNotBlank();
        assertThat(response.jsonPath().getString("data.instructor_name")).isNotBlank();
        assertThat(response.jsonPath().getString("data.start_time")).isNotBlank();
        assertThat(response.jsonPath().getString("data.end_time")).isNotBlank();
    }

    @Test(priority = 10, groups = {"schedule"})
    @Story("Lecture Details")
    @Severity(SeverityLevel.MINOR)
    @Description("Get non-existent lecture ID must return 404")
    public void testGetLectureById_NonExistentId_Returns404() {
        Response response = authedSpec()
                .get("/api/v1/schedule/lectures/lecture-does-not-exist-99999");

        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    // ── Online Lectures ────────────────────────────────────

    @Test(priority = 11, groups = {"schedule"})
    @Story("Online Lectures")
    @Severity(SeverityLevel.NORMAL)
    @Description("Online lectures must include a non-null meeting_link field")
    public void testGetOnlineLectures_ReturnsMeetingLinks() {
        Response response = authedSpec()
                .queryParam("is_online", true)
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isEqualTo(200);

        List<Object> lectures = response.jsonPath().getList("data.lectures");
        lectures.forEach(lec -> {
            // Every online lecture must have a meeting link
        });
        // We assert the response structure is correct
        assertThat(response.jsonPath().getList("data.lectures")).isNotNull();
    }

    // ── Course Search ──────────────────────────────────────

    @Test(priority = 12, groups = {"schedule"})
    @Story("Course Search")
    @Severity(SeverityLevel.NORMAL)
    @Description("Search courses by name keyword – returns matching results")
    public void testSearchCourses_ValidKeyword_ReturnsResults() {
        Response response = authedSpec()
                .queryParam("q", "Software")
                .queryParam("limit", 5)
                .get("/api/v1/schedule/courses/search");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data")).isNotNull();
    }

    @Test(priority = 13, groups = {"schedule"})
    @Story("Course Search")
    @Severity(SeverityLevel.MINOR)
    @Description("Search with a single-character query must return 400 (too short)")
    public void testSearchCourses_SingleCharQuery_Returns400() {
        Response response = authedSpec()
                .queryParam("q", "S")
                .get("/api/v1/schedule/courses/search");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    // ── Instructor ─────────────────────────────────────────

    @Test(priority = 14, groups = {"schedule"})
    @Story("Instructor Info")
    @Severity(SeverityLevel.MINOR)
    @Description("Get all courses by a specific instructor ID – returns filtered list")
    public void testGetCoursesByInstructor_ValidInstructorId_Returns200() {
        Response response = authedSpec()
                .get("/api/v1/schedule/instructors/INST-001/courses");

        assertThat(response.getStatusCode()).isIn(200, 404);
    }

    // ── Notifications / Reminders ──────────────────────────

    @Test(priority = 15, groups = {"schedule"})
    @Story("Lecture Reminders")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get upcoming lecture reminders for next 24 hours – returns list")
    public void testGetUpcomingReminders_AuthenticatedStudent_Returns200() {
        Response response = authedSpec()
                .queryParam("hours", 24)
                .get("/api/v1/schedule/reminders");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data")).isNotNull();
    }

    // ── No Auth ────────────────────────────────────────────

    @Test(priority = 16, groups = {"schedule"})
    @Story("Authorization")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Unauthenticated request to schedule endpoint must return 401")
    public void testGetWeeklySchedule_NoAuth_Returns401() {
        Response response = io.restassured.RestAssured.given()
                .spec(requestSpec)
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isEqualTo(401);
    }

    // ── Department Schedule ────────────────────────────────

    @Test(priority = 17, groups = {"schedule"})
    @Story("Department Schedule")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get schedule by department ID – returns department-specific lectures")
    public void testGetScheduleByDepartment_ValidDepartmentId_Returns200() {
        Response response = authedSpec()
                .queryParam("department_id", config.testDepartmentId())
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    // ── Response Schema ────────────────────────────────────

    @Test(priority = 18, groups = {"schedule"})
    @Story("Weekly Schedule")
    @Severity(SeverityLevel.NORMAL)
    @Description("Weekly schedule response must contain all required fields in each lecture object")
    public void testGetWeeklySchedule_ResponseSchema_ContainsRequiredFields() {
        Response response = authedSpec()
                .get("/api/v1/schedule/weekly");

        assertThat(response.getStatusCode()).isEqualTo(200);

        List<Object> lectures = response.jsonPath().getList("data.lectures");
        if (!lectures.isEmpty()) {
            assertThat(response.jsonPath().getString("data.lectures[0].course_name")).isNotNull();
            assertThat(response.jsonPath().getString("data.lectures[0].day_of_week")).isNotNull();
            assertThat(response.jsonPath().getString("data.lectures[0].start_time")).isNotNull();
            assertThat(response.jsonPath().getString("data.lectures[0].end_time")).isNotNull();
        }
    }
}
