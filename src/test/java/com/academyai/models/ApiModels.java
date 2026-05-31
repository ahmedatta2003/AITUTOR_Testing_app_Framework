package com.academyai.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ═══════════════════════════════════════════════════════════
//  AUTH MODELS
// ═══════════════════════════════════════════════════════════

class AuthModels {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        private String name;
        private String email;
        private String password;
        @JsonProperty("university_id") private String universityId;
        @JsonProperty("faculty_id")    private String facultyId;
        @JsonProperty("academic_year") private int academicYear;
        @JsonProperty("student_id")    private String studentId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthResponse {
        private String token;
        @JsonProperty("refresh_token") private String refreshToken;
        private UserDto user;
        private String message;
    }
}

// ═══════════════════════════════════════════════════════════
//  USER / STUDENT MODEL
// ═══════════════════════════════════════════════════════════

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class UserDto {
    private String id;
    private String name;
    private String email;
    @JsonProperty("university_id")  private String universityId;
    @JsonProperty("faculty_name")   private String facultyName;
    @JsonProperty("academic_year")  private int academicYear;
    @JsonProperty("student_code")   private String studentCode;
    @JsonProperty("profile_image")  private String profileImage;
    @JsonProperty("is_verified")    private boolean isVerified;
    @JsonProperty("created_at")     private String createdAt;
}

// ═══════════════════════════════════════════════════════════
//  AI CHAT MODELS
// ═══════════════════════════════════════════════════════════

class AiModels {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AiChatRequest {
        private String message;
        @JsonProperty("session_id")   private String sessionId;
        @JsonProperty("subject_hint") private String subjectHint;
        private String language;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiChatResponse {
        @JsonProperty("session_id")    private String sessionId;
        private String reply;
        private double confidence;
        @JsonProperty("tokens_used")   private int tokensUsed;
        @JsonProperty("response_time") private long responseTimeMs;
        private List<String> sources;
        private String model;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiSessionHistory {
        @JsonProperty("session_id") private String sessionId;
        private List<AiMessage> messages;
        @JsonProperty("created_at") private String createdAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiMessage {
        private String role;  // "user" | "assistant"
        private String content;
        private String timestamp;
    }
}

// ═══════════════════════════════════════════════════════════
//  SOCIAL FEED MODELS
// ═══════════════════════════════════════════════════════════

class SocialModels {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreatePostRequest {
        private String content;
        @JsonProperty("post_type")  private String postType; // "text"|"image"|"poll"
        private List<String> tags;
        @JsonProperty("faculty_id") private String facultyId;
        @JsonProperty("is_anonymous") private boolean isAnonymous;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostDto {
        private String id;
        private String content;
        @JsonProperty("author_name")  private String authorName;
        @JsonProperty("author_id")    private String authorId;
        @JsonProperty("likes_count")  private int likesCount;
        @JsonProperty("comments_count") private int commentsCount;
        @JsonProperty("is_liked")     private boolean isLiked;
        @JsonProperty("created_at")   private String createdAt;
        private List<String> tags;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CommentRequest {
        @JsonProperty("post_id") private String postId;
        private String content;
    }
}

// ═══════════════════════════════════════════════════════════
//  LECTURE SCHEDULE MODELS
// ═══════════════════════════════════════════════════════════

class ScheduleModels {

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LectureDto {
        private String id;
        @JsonProperty("course_name")   private String courseName;
        @JsonProperty("course_code")   private String courseCode;
        @JsonProperty("instructor_name") private String instructorName;
        @JsonProperty("day_of_week")   private String dayOfWeek;
        @JsonProperty("start_time")    private String startTime;
        @JsonProperty("end_time")      private String endTime;
        private String room;
        @JsonProperty("is_online")     private boolean isOnline;
        @JsonProperty("meeting_link")  private String meetingLink;
        @JsonProperty("week_number")   private int weekNumber;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeeklyScheduleResponse {
        @JsonProperty("academic_year") private int academicYear;
        @JsonProperty("semester")      private int semester;
        @JsonProperty("week_number")   private int weekNumber;
        private List<LectureDto> lectures;
    }
}

// ═══════════════════════════════════════════════════════════
//  STUDENT CHAT MODELS
// ═══════════════════════════════════════════════════════════

class ChatModels {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SendMessageRequest {
        @JsonProperty("conversation_id") private String conversationId;
        private String content;
        @JsonProperty("message_type") private String messageType; // "text"|"image"|"file"
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MessageDto {
        private String id;
        @JsonProperty("sender_id")   private String senderId;
        @JsonProperty("sender_name") private String senderName;
        private String content;
        @JsonProperty("message_type") private String messageType;
        @JsonProperty("sent_at")     private String sentAt;
        @JsonProperty("is_read")     private boolean isRead;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ConversationDto {
        private String id;
        @JsonProperty("other_user_name")  private String otherUserName;
        @JsonProperty("last_message")     private String lastMessage;
        @JsonProperty("last_message_time") private String lastMessageTime;
        @JsonProperty("unread_count")     private int unreadCount;
    }
}

// ═══════════════════════════════════════════════════════════
//  GENERIC API RESPONSE WRAPPER
// ═══════════════════════════════════════════════════════════

@Data @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private int statusCode;
    private String error;
}
