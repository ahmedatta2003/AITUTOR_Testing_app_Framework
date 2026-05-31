package com.academyai.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Sources;

@LoadPolicy(LoadType.MERGE)
@Sources({
    "system:properties",
    "classpath:config.properties"
})
public interface FrameworkConfig extends Config {

    // ── Base URLs ──────────────────────────────────────────
    @Key("base.url")
    @DefaultValue("https://api.academyai.edu.eg")
    String baseUrl();

    @Key("ai.base.url")
    @DefaultValue("https://ai.academyai.edu.eg")
    String aiBaseUrl();

    @Key("chat.ws.url")
    @DefaultValue("wss://chat.academyai.edu.eg/ws")
    String chatWsUrl();

    // ── Auth ───────────────────────────────────────────────
    @Key("admin.email")
    @DefaultValue("admin@academyai.edu.eg")
    String adminEmail();

    @Key("admin.password")
    @DefaultValue("Admin@123")
    String adminPassword();

    @Key("student.email")
    @DefaultValue("student.test@academyai.edu.eg")
    String studentEmail();

    @Key("student.password")
    @DefaultValue("Student@123")
    String studentPassword();

    // ── Timeouts ───────────────────────────────────────────
    @Key("connection.timeout")
    @DefaultValue("10000")
    int connectionTimeout();

    @Key("read.timeout")
    @DefaultValue("30000")
    int readTimeout();

    @Key("ai.response.timeout")
    @DefaultValue("60000")
    int aiResponseTimeout();

    // ── Performance Thresholds ─────────────────────────────
    @Key("perf.auth.max.response.ms")
    @DefaultValue("2000")
    int perfAuthMaxResponseMs();

    @Key("perf.ai.max.response.ms")
    @DefaultValue("10000")
    int perfAiMaxResponseMs();

    @Key("perf.chat.max.response.ms")
    @DefaultValue("1000")
    int perfChatMaxResponseMs();

    @Key("perf.schedule.max.response.ms")
    @DefaultValue("1500")
    int perfScheduleMaxResponseMs();

    @Key("perf.social.max.response.ms")
    @DefaultValue("2000")
    int perfSocialMaxResponseMs();

    // ── Test Data ──────────────────────────────────────────
    @Key("test.university.id")
    @DefaultValue("ELSHOROUK-001")
    String testUniversityId();

    @Key("test.faculty.id")
    @DefaultValue("CS-FACULTY-001")
    String testFacultyId();

    @Key("test.department.id")
    @DefaultValue("SE-DEPT-001")
    String testDepartmentId();
}
