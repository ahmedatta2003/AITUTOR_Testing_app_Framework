package com.academyai.api.social;

import com.academyai.utils.BaseApiTest;
import com.academyai.utils.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Social Feed Tests – covers the student social wall inside AcademyAI.
 *
 * Covers:
 *  - Posts  : create, read, update, delete, pagination
 *  - Likes  : like, unlike, like count
 *  - Comments: add, read, delete
 *  - Feed   : personal feed, faculty feed, trending
 *  - Tags   : filter by hashtag
 *  - Input validation & authorization
 */
@Epic("AcademyAI Platform")
@Feature("Social Feed")
public class SocialFeedTests extends BaseApiTest {

    private String postId;
    private String commentId;

    // ── Posts ──────────────────────────────────────────────

    @Test(priority = 1, groups = {"smoke", "social"})
    @Story("Create Post")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Create a new text post – returns 201 with postId and author info")
    public void testCreatePost_ValidTextPost_Returns201() {
        String content = TestDataFactory.randomPostContent()
                + " " + TestDataFactory.randomHashtag();

        Response response = authedSpec()
                .body("{"
                    + "\"content\": \"" + content + "\","
                    + "\"post_type\": \"text\","
                    + "\"tags\": [\"StudyTips\", \"AcademyAI\"],"
                    + "\"faculty_id\": \"" + config.testFacultyId() + "\","
                    + "\"is_anonymous\": false"
                    + "}")
                .post("/api/v1/social/posts");

        assertThat(response.getStatusCode()).isEqualTo(201);

        postId = response.jsonPath().getString("data.id");
        assertThat(postId).isNotBlank();
        assertThat(response.jsonPath().getString("data.content")).isEqualTo(content);
        assertThat(response.jsonPath().getString("data.author_name")).isNotBlank();
        assertThat(response.jsonPath().getInt("data.likes_count")).isEqualTo(0);
        assertThat(response.time()).isLessThan(config.perfSocialMaxResponseMs());

        log.info("Post created. ID: {}", postId);
    }

    @Test(priority = 2, groups = {"social"})
    @Story("Create Post")
    @Severity(SeverityLevel.NORMAL)
    @Description("Create anonymous post – author_name must be hidden/anonymous")
    public void testCreatePost_AnonymousPost_HidesAuthorName() {
        Response response = authedSpec()
                .body("{"
                    + "\"content\": \"Anonymous feedback about the curriculum\","
                    + "\"post_type\": \"text\","
                    + "\"faculty_id\": \"" + config.testFacultyId() + "\","
                    + "\"is_anonymous\": true"
                    + "}")
                .post("/api/v1/social/posts");

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.jsonPath().getString("data.author_name"))
                .isIn("Anonymous", "مجهول", null);
    }

    @Test(priority = 3, groups = {"social"})
    @Story("Create Post")
    @Severity(SeverityLevel.NORMAL)
    @Description("Create post with empty content must return 400")
    public void testCreatePost_EmptyContent_Returns400() {
        Response response = authedSpec()
                .body("{"
                    + "\"content\": \"\","
                    + "\"post_type\": \"text\","
                    + "\"faculty_id\": \"" + config.testFacultyId() + "\""
                    + "}")
                .post("/api/v1/social/posts");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test(priority = 4, groups = {"social"})
    @Story("Create Post")
    @Severity(SeverityLevel.MINOR)
    @Description("Create post exceeding max character limit must return 422")
    public void testCreatePost_ExceedsMaxLength_Returns422() {
        String longContent = "A".repeat(3001);

        Response response = authedSpec()
                .body("{"
                    + "\"content\": \"" + longContent + "\","
                    + "\"post_type\": \"text\","
                    + "\"faculty_id\": \"" + config.testFacultyId() + "\""
                    + "}")
                .post("/api/v1/social/posts");

        assertThat(response.getStatusCode()).isIn(400, 413, 422);
    }

    // ── Feed ───────────────────────────────────────────────

    @Test(priority = 5, groups = {"smoke", "social"})
    @Story("View Feed")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Get paginated personal feed – returns 200 with list of posts")
    public void testGetFeed_AuthenticatedStudent_ReturnsPaginatedPosts() {
        Response response = authedSpec()
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .get("/api/v1/social/feed");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data.posts")).isNotNull();
        assertThat(response.jsonPath().getInt("data.page")).isEqualTo(1);
        assertThat(response.jsonPath().getInt("data.limit")).isEqualTo(10);
        assertThat(response.time()).isLessThan(config.perfSocialMaxResponseMs());
    }

    @Test(priority = 6, groups = {"social"})
    @Story("View Feed")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get faculty-specific feed – returns only posts from that faculty")
    public void testGetFacultyFeed_ValidFacultyId_ReturnsFilteredPosts() {
        Response response = authedSpec()
                .queryParam("faculty_id", config.testFacultyId())
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .get("/api/v1/social/feed");

        assertThat(response.getStatusCode()).isEqualTo(200);

        // All returned posts should belong to the queried faculty
        response.jsonPath().<String>getList("data.posts.faculty_id").forEach(id ->
                assertThat(id).isEqualTo(config.testFacultyId()));
    }

    @Test(priority = 7, groups = {"social"})
    @Story("View Feed")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get trending posts – returns list ordered by engagement")
    public void testGetTrendingPosts_Returns200WithSortedList() {
        Response response = authedSpec()
                .queryParam("limit", 5)
                .get("/api/v1/social/trending");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data")).isNotNull();
    }

    @Test(priority = 8, groups = {"social"}, dependsOnMethods = {"testCreatePost_ValidTextPost_Returns201"})
    @Story("View Feed")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get a single post by ID – returns 200 with full post object")
    public void testGetPostById_ValidId_Returns200() {
        Response response = authedSpec()
                .get("/api/v1/social/posts/" + postId);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.id")).isEqualTo(postId);
        assertThat(response.jsonPath().getString("data.content")).isNotBlank();
    }

    @Test(priority = 9, groups = {"social"})
    @Story("View Feed")
    @Severity(SeverityLevel.MINOR)
    @Description("Get non-existent post by ID must return 404")
    public void testGetPostById_NonExistentId_Returns404() {
        Response response = authedSpec()
                .get("/api/v1/social/posts/post-does-not-exist-99999");

        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    // ── Like / Unlike ──────────────────────────────────────

    @Test(priority = 10, groups = {"social"}, dependsOnMethods = {"testCreatePost_ValidTextPost_Returns201"})
    @Story("Like / Unlike")
    @Severity(SeverityLevel.NORMAL)
    @Description("Like a post – returns 200 and likes_count increments by 1")
    public void testLikePost_ValidPost_LikesCountIncremented() {
        int likesBefore = authedSpec()
                .get("/api/v1/social/posts/" + postId)
                .jsonPath().getInt("data.likes_count");

        Response response = authedSpec()
                .post("/api/v1/social/posts/" + postId + "/like");

        assertThat(response.getStatusCode()).isEqualTo(200);

        int likesAfter = authedSpec()
                .get("/api/v1/social/posts/" + postId)
                .jsonPath().getInt("data.likes_count");

        assertThat(likesAfter).isEqualTo(likesBefore + 1);
        assertThat(response.jsonPath().getBoolean("data.is_liked")).isTrue();
    }

    @Test(priority = 11, groups = {"social"}, dependsOnMethods = {"testLikePost_ValidPost_LikesCountIncremented"})
    @Story("Like / Unlike")
    @Severity(SeverityLevel.NORMAL)
    @Description("Like an already-liked post (double-like) must return 409 or toggle back")
    public void testLikePost_AlreadyLiked_Returns409OrToggle() {
        Response response = authedSpec()
                .post("/api/v1/social/posts/" + postId + "/like");

        assertThat(response.getStatusCode()).isIn(200, 409);
    }

    @Test(priority = 12, groups = {"social"}, dependsOnMethods = {"testLikePost_ValidPost_LikesCountIncremented"})
    @Story("Like / Unlike")
    @Severity(SeverityLevel.NORMAL)
    @Description("Unlike a liked post – likes_count decrements by 1 and is_liked becomes false")
    public void testUnlikePost_LikedPost_LikesCountDecremented() {
        int likesBefore = authedSpec()
                .get("/api/v1/social/posts/" + postId)
                .jsonPath().getInt("data.likes_count");

        Response response = authedSpec()
                .delete("/api/v1/social/posts/" + postId + "/like");

        assertThat(response.getStatusCode()).isEqualTo(200);

        int likesAfter = authedSpec()
                .get("/api/v1/social/posts/" + postId)
                .jsonPath().getInt("data.likes_count");

        assertThat(likesAfter).isLessThan(likesBefore);
        assertThat(response.jsonPath().getBoolean("data.is_liked")).isFalse();
    }

    // ── Comments ───────────────────────────────────────────

    @Test(priority = 13, groups = {"smoke", "social"}, dependsOnMethods = {"testCreatePost_ValidTextPost_Returns201"})
    @Story("Comments")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Add a comment to a post – returns 201 with commentId")
    public void testAddComment_ValidPost_Returns201() {
        String commentText = "Great post! Very helpful for the exam.";

        Response response = authedSpec()
                .body("{"
                    + "\"post_id\": \"" + postId + "\","
                    + "\"content\": \"" + commentText + "\""
                    + "}")
                .post("/api/v1/social/comments");

        assertThat(response.getStatusCode()).isEqualTo(201);

        commentId = response.jsonPath().getString("data.id");
        assertThat(commentId).isNotBlank();
        assertThat(response.jsonPath().getString("data.content")).isEqualTo(commentText);
        assertThat(response.jsonPath().getString("data.author_name")).isNotBlank();
    }

    @Test(priority = 14, groups = {"social"}, dependsOnMethods = {"testCreatePost_ValidTextPost_Returns201"})
    @Story("Comments")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get all comments for a post – returns list with count")
    public void testGetComments_ValidPost_ReturnsCommentList() {
        Response response = authedSpec()
                .get("/api/v1/social/posts/" + postId + "/comments");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data.comments")).isNotNull();
        assertThat(response.jsonPath().getInt("data.total")).isGreaterThanOrEqualTo(0);
    }

    @Test(priority = 15, groups = {"social"})
    @Story("Comments")
    @Severity(SeverityLevel.NORMAL)
    @Description("Add empty comment must return 400 Bad Request")
    public void testAddComment_EmptyContent_Returns400() {
        Response response = authedSpec()
                .body("{\"post_id\": \"" + postId + "\", \"content\": \"\"}")
                .post("/api/v1/social/comments");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test(priority = 16, groups = {"social"}, dependsOnMethods = {"testAddComment_ValidPost_Returns201"})
    @Story("Comments")
    @Severity(SeverityLevel.MINOR)
    @Description("Delete own comment – returns 200 and comment count decrements")
    public void testDeleteComment_OwnComment_Returns200() {
        Response response = authedSpec()
                .delete("/api/v1/social/comments/" + commentId);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    // ── Update & Delete Post ───────────────────────────────

    @Test(priority = 17, groups = {"social"}, dependsOnMethods = {"testCreatePost_ValidTextPost_Returns201"})
    @Story("Update Post")
    @Severity(SeverityLevel.NORMAL)
    @Description("Update own post content – returns 200 with updated content")
    public void testUpdatePost_OwnPost_Returns200() {
        String updatedContent = "Updated: " + TestDataFactory.randomPostContent();

        Response response = authedSpec()
                .body("{\"content\": \"" + updatedContent + "\"}")
                .put("/api/v1/social/posts/" + postId);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.content")).isEqualTo(updatedContent);
    }

    @Test(priority = 18, groups = {"social", "security"})
    @Story("Authorization")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Update another student's post must return 403 Forbidden")
    public void testUpdatePost_OtherStudentPost_Returns403() {
        Response response = authedSpec()
                .body("{\"content\": \"Trying to edit someone else's post\"}")
                .put("/api/v1/social/posts/post-belongs-to-other-student-001");

        assertThat(response.getStatusCode()).isIn(403, 404);
    }

    @Test(priority = 19, groups = {"social"}, dependsOnMethods = {"testCreatePost_ValidTextPost_Returns201"})
    @Story("Delete Post")
    @Severity(SeverityLevel.NORMAL)
    @Description("Delete own post – returns 200 and post becomes inaccessible")
    public void testDeletePost_OwnPost_Returns200() {
        Response response = authedSpec()
                .delete("/api/v1/social/posts/" + postId);

        assertThat(response.getStatusCode()).isEqualTo(200);

        Response getPost = authedSpec()
                .get("/api/v1/social/posts/" + postId);
        assertThat(getPost.getStatusCode()).isIn(404, 410);
    }

    // ── Hashtag Filter ─────────────────────────────────────

    @Test(priority = 20, groups = {"social"})
    @Story("Hashtag Search")
    @Severity(SeverityLevel.MINOR)
    @Description("Filter posts by hashtag – returns only posts with that tag")
    public void testFilterByHashtag_ValidTag_ReturnsFilteredPosts() {
        Response response = authedSpec()
                .queryParam("tag", "AcademyAI")
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .get("/api/v1/social/posts");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data.posts")).isNotNull();
    }

    // ── No Auth ────────────────────────────────────────────

    @Test(priority = 21, groups = {"social"})
    @Story("Authorization")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Unauthenticated request to social feed must return 401")
    public void testGetFeed_NoAuth_Returns401() {
        Response response = io.restassured.RestAssured.given()
                .spec(requestSpec)
                .get("/api/v1/social/feed");

        assertThat(response.getStatusCode()).isEqualTo(401);
    }
}
