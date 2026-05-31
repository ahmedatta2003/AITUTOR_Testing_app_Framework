package com.academyai.api.chat;

import com.academyai.utils.BaseApiTest;
import com.academyai.utils.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Student-to-Student Chat Tests.
 *
 * Covers:
 *  - Conversations: list, create, fetch
 *  - Messages     : send, receive, pagination, read status
 *  - Group chat   : create group, add/remove members, send
 *  - Edge cases   : empty message, max length, unauthorized access
 *  - Performance  : response time assertions
 */
@Epic("AcademyAI Platform")
@Feature("Student Chat")
public class StudentChatTests extends BaseApiTest {

    private String conversationId;
    private String messageId;
    private String groupConversationId;

    @BeforeClass(alwaysRun = true)
    @Override
    public void setupSpec() {
        super.setupSpec();
        log.info("StudentChatTests – setting up with token: {}",
                bearerToken != null ? bearerToken.substring(0, 10) + "..." : "NULL");
    }

    // ── Conversations ──────────────────────────────────────

    @Test(priority = 1, groups = {"smoke", "chat"})
    @Story("Conversations")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Get all conversations for the current student – must return 200 and list")
    public void testGetConversations_AuthenticatedStudent_Returns200() {
        Response response = authedSpec()
                .get("/api/v1/chat/conversations");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data")).isNotNull();
        assertThat(response.time()).isLessThan(config.perfChatMaxResponseMs());
    }

    @Test(priority = 2, groups = {"chat"})
    @Story("Conversations")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Start a new direct conversation with another student – returns 201 with conversationId")
    public void testCreateConversation_ValidPeer_Returns201() {
        Response response = authedSpec()
                .body("{"
                    + "\"peer_student_id\": \"STU-PEER-001\","
                    + "\"type\": \"direct\""
                    + "}")
                .post("/api/v1/chat/conversations");

        assertThat(response.getStatusCode()).isIn(200, 201);

        conversationId = response.jsonPath().getString("data.id");
        assertThat(conversationId).isNotBlank();

        assertThat(response.jsonPath().getString("data.type"))
                .isEqualToIgnoringCase("direct");

        log.info("Created conversationId: {}", conversationId);
    }

    @Test(priority = 3, groups = {"chat"}, dependsOnMethods = {"testCreateConversation_ValidPeer_Returns201"})
    @Story("Conversations")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get a specific conversation by ID – returns 200 with correct conversation object")
    public void testGetConversationById_ValidId_Returns200() {
        Response response = authedSpec()
                .get("/api/v1/chat/conversations/" + conversationId);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getString("data.id")).isEqualTo(conversationId);
    }

    @Test(priority = 4, groups = {"chat"})
    @Story("Conversations")
    @Severity(SeverityLevel.NORMAL)
    @Description("Accessing a non-existent conversation must return 404")
    public void testGetConversation_NonExistentId_Returns404() {
        Response response = authedSpec()
                .get("/api/v1/chat/conversations/conv-does-not-exist-00000");

        assertThat(response.getStatusCode()).isEqualTo(404);
    }

    @Test(priority = 5, groups = {"chat"})
    @Story("Conversations")
    @Severity(SeverityLevel.NORMAL)
    @Description("Create conversation with self must return 400 Bad Request")
    public void testCreateConversation_WithSelf_Returns400() {
        // Use own student ID
        Response profileResp = authedSpec().get("/api/v1/auth/profile");
        String ownId = profileResp.jsonPath().getString("data.id");

        Response response = authedSpec()
                .body("{\"peer_student_id\": \"" + ownId + "\", \"type\": \"direct\"}")
                .post("/api/v1/chat/conversations");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    // ── Messages ───────────────────────────────────────────

    @Test(priority = 6, groups = {"smoke", "chat"}, dependsOnMethods = {"testCreateConversation_ValidPeer_Returns201"})
    @Story("Send & Receive Messages")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Send a text message in an existing conversation – returns 201 with messageId")
    public void testSendMessage_ValidTextMessage_Returns201() {
        String content = TestDataFactory.randomChatMessage();

        Response response = authedSpec()
                .body("{"
                    + "\"conversation_id\": \"" + conversationId + "\","
                    + "\"content\": \"" + content + "\","
                    + "\"message_type\": \"text\""
                    + "}")
                .post("/api/v1/chat/messages");

        assertThat(response.getStatusCode()).isEqualTo(201);

        messageId = response.jsonPath().getString("data.id");
        assertThat(messageId).isNotBlank();

        assertThat(response.jsonPath().getString("data.content")).isEqualTo(content);
        assertThat(response.jsonPath().getString("data.message_type")).isEqualTo("text");
        assertThat(response.time()).isLessThan(config.perfChatMaxResponseMs());

        log.info("Message sent. ID: {} | content: {}", messageId, content);
    }

    @Test(priority = 7, groups = {"chat"}, dependsOnMethods = {"testCreateConversation_ValidPeer_Returns201"})
    @Story("Send & Receive Messages")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get message history of a conversation – returns paginated messages")
    public void testGetMessages_ValidConversation_ReturnsPaginatedList() {
        Response response = authedSpec()
                .queryParam("page", 1)
                .queryParam("limit", 20)
                .get("/api/v1/chat/conversations/" + conversationId + "/messages");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data.messages")).isNotNull();
        assertThat(response.jsonPath().getInt("data.page")).isEqualTo(1);
        assertThat(response.jsonPath().getInt("data.limit")).isEqualTo(20);
    }

    @Test(priority = 8, groups = {"chat"}, dependsOnMethods = {"testSendMessage_ValidTextMessage_Returns201"})
    @Story("Send & Receive Messages")
    @Severity(SeverityLevel.NORMAL)
    @Description("Mark a message as read – returns 200 and is_read becomes true")
    public void testMarkMessageRead_ValidMessageId_Returns200() {
        Response response = authedSpec()
                .put("/api/v1/chat/messages/" + messageId + "/read");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("data.is_read")).isTrue();
    }

    @Test(priority = 9, groups = {"chat"}, dependsOnMethods = {"testCreateConversation_ValidPeer_Returns201"})
    @Story("Send & Receive Messages")
    @Severity(SeverityLevel.NORMAL)
    @Description("Mark all messages in a conversation as read – returns 200")
    public void testMarkAllMessagesRead_ValidConversation_Returns200() {
        Response response = authedSpec()
                .put("/api/v1/chat/conversations/" + conversationId + "/read-all");

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test(priority = 10, groups = {"chat"}, dependsOnMethods = {"testSendMessage_ValidTextMessage_Returns201"})
    @Story("Send & Receive Messages")
    @Severity(SeverityLevel.MINOR)
    @Description("Delete a sent message – returns 200 and message becomes inaccessible")
    public void testDeleteMessage_OwnMessage_Returns200() {
        Response response = authedSpec()
                .delete("/api/v1/chat/messages/" + messageId);

        assertThat(response.getStatusCode()).isEqualTo(200);

        // Verify deletion
        Response getMsg = authedSpec()
                .get("/api/v1/chat/messages/" + messageId);
        assertThat(getMsg.getStatusCode()).isIn(404, 410);
    }

    // ── Input Validation ───────────────────────────────────

    @Test(priority = 11, groups = {"chat"}, dependsOnMethods = {"testCreateConversation_ValidPeer_Returns201"})
    @Story("Input Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Send empty message must return 400 Bad Request")
    public void testSendMessage_EmptyContent_Returns400() {
        Response response = authedSpec()
                .body("{"
                    + "\"conversation_id\": \"" + conversationId + "\","
                    + "\"content\": \"\","
                    + "\"message_type\": \"text\""
                    + "}")
                .post("/api/v1/chat/messages");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    @Test(priority = 12, groups = {"chat"}, dependsOnMethods = {"testCreateConversation_ValidPeer_Returns201"})
    @Story("Input Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Send message exceeding max length (2000 chars) must return 422")
    public void testSendMessage_OversizedContent_Returns422() {
        String bigMsg = "M".repeat(2001);

        Response response = authedSpec()
                .body("{"
                    + "\"conversation_id\": \"" + conversationId + "\","
                    + "\"content\": \"" + bigMsg + "\","
                    + "\"message_type\": \"text\""
                    + "}")
                .post("/api/v1/chat/messages");

        assertThat(response.getStatusCode()).isIn(400, 413, 422);
    }

    @Test(priority = 13, groups = {"chat", "security"})
    @Story("Input Validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Send message to a conversation you don't belong to must return 403 Forbidden")
    public void testSendMessage_UnauthorizedConversation_Returns403() {
        Response response = authedSpec()
                .body("{"
                    + "\"conversation_id\": \"conv-other-students-only-999\","
                    + "\"content\": \"Unauthorized access attempt\","
                    + "\"message_type\": \"text\""
                    + "}")
                .post("/api/v1/chat/messages");

        assertThat(response.getStatusCode()).isIn(403, 404);
    }

    @Test(priority = 14, groups = {"chat"})
    @Story("Input Validation")
    @Severity(SeverityLevel.MINOR)
    @Description("Unauthenticated user cannot access chat endpoints – returns 401")
    public void testGetConversations_NoAuth_Returns401() {
        Response response = io.restassured.RestAssured.given()
                .spec(requestSpec)
                .get("/api/v1/chat/conversations");

        assertThat(response.getStatusCode()).isEqualTo(401);
    }

    // ── Group Chat ─────────────────────────────────────────

    @Test(priority = 15, groups = {"chat"})
    @Story("Group Chat")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Create a group conversation with multiple students – returns 201 with group ID")
    public void testCreateGroupConversation_ValidMembers_Returns201() {
        Response response = authedSpec()
                .body("{"
                    + "\"type\": \"group\","
                    + "\"name\": \"Software Testing Study Group\","
                    + "\"member_ids\": [\"STU-PEER-001\", \"STU-PEER-002\", \"STU-PEER-003\"]"
                    + "}")
                .post("/api/v1/chat/conversations");

        assertThat(response.getStatusCode()).isIn(200, 201);

        groupConversationId = response.jsonPath().getString("data.id");
        assertThat(groupConversationId).isNotBlank();

        assertThat(response.jsonPath().getString("data.type")).isEqualToIgnoringCase("group");
        assertThat(response.jsonPath().getString("data.name"))
                .isEqualTo("Software Testing Study Group");

        log.info("Group chat created. ID: {}", groupConversationId);
    }

    @Test(priority = 16, groups = {"chat"}, dependsOnMethods = {"testCreateGroupConversation_ValidMembers_Returns201"})
    @Story("Group Chat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Send a message to a group conversation – returns 201")
    public void testSendMessageToGroup_ValidGroup_Returns201() {
        Response response = authedSpec()
                .body("{"
                    + "\"conversation_id\": \"" + groupConversationId + "\","
                    + "\"content\": \"Hey team! Who's ready for the exam?\","
                    + "\"message_type\": \"text\""
                    + "}")
                .post("/api/v1/chat/messages");

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.jsonPath().getString("data.content"))
                .contains("exam");
    }

    @Test(priority = 17, groups = {"chat"}, dependsOnMethods = {"testCreateGroupConversation_ValidMembers_Returns201"})
    @Story("Group Chat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Add a new member to existing group – returns 200")
    public void testAddMemberToGroup_ValidMember_Returns200() {
        Response response = authedSpec()
                .body("{\"student_id\": \"STU-PEER-004\"}")
                .post("/api/v1/chat/conversations/" + groupConversationId + "/members");

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test(priority = 18, groups = {"chat"}, dependsOnMethods = {"testCreateGroupConversation_ValidMembers_Returns201"})
    @Story("Group Chat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Remove a member from group – returns 200")
    public void testRemoveMemberFromGroup_ValidMember_Returns200() {
        Response response = authedSpec()
                .delete("/api/v1/chat/conversations/" + groupConversationId + "/members/STU-PEER-003");

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @Test(priority = 19, groups = {"chat"})
    @Story("Group Chat")
    @Severity(SeverityLevel.NORMAL)
    @Description("Create group with too few members (less than 2) must return 400")
    public void testCreateGroupConversation_TooFewMembers_Returns400() {
        Response response = authedSpec()
                .body("{"
                    + "\"type\": \"group\","
                    + "\"name\": \"Empty Group\","
                    + "\"member_ids\": [\"STU-PEER-001\"]"
                    + "}")
                .post("/api/v1/chat/conversations");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    // ── Search ─────────────────────────────────────────────

    @Test(priority = 20, groups = {"chat"})
    @Story("Chat Search")
    @Severity(SeverityLevel.MINOR)
    @Description("Search students by name to start a new conversation – returns matching list")
    public void testSearchStudents_ValidQuery_ReturnsResults() {
        Response response = authedSpec()
                .queryParam("q", "Ahmed")
                .queryParam("limit", 10)
                .get("/api/v1/chat/students/search");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getList("data")).isNotNull();
    }

    @Test(priority = 21, groups = {"chat"})
    @Story("Chat Search")
    @Severity(SeverityLevel.MINOR)
    @Description("Search with empty query must return 400 Bad Request")
    public void testSearchStudents_EmptyQuery_Returns400() {
        Response response = authedSpec()
                .queryParam("q", "")
                .get("/api/v1/chat/students/search");

        assertThat(response.getStatusCode()).isEqualTo(400);
    }

    // ── Typing Indicator ───────────────────────────────────

    @Test(priority = 22, groups = {"chat"}, dependsOnMethods = {"testCreateConversation_ValidPeer_Returns201"})
    @Story("Real-time Features")
    @Severity(SeverityLevel.MINOR)
    @Description("Post typing indicator event to conversation – returns 200")
    public void testTypingIndicator_InActiveConversation_Returns200() {
        Response response = authedSpec()
                .body("{\"is_typing\": true}")
                .post("/api/v1/chat/conversations/" + conversationId + "/typing");

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    // ── Unread Count ───────────────────────────────────────

    @Test(priority = 23, groups = {"chat"})
    @Story("Unread Messages")
    @Severity(SeverityLevel.NORMAL)
    @Description("Get total unread message count across all conversations – returns valid count")
    public void testGetUnreadCount_AuthenticatedStudent_ReturnsCount() {
        Response response = authedSpec()
                .get("/api/v1/chat/unread-count");

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getInt("data.unread_count")).isGreaterThanOrEqualTo(0);
    }
}
