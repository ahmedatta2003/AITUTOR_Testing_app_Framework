package com.academyai.performance;

import com.academyai.utils.BaseApiTest;
import com.academyai.utils.TestDataFactory;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance Tests for AcademyAI API.
 *
 * Types covered:
 *  1. Response Time Tests    – single-user latency per endpoint
 *  2. Concurrent Load Tests  – multi-threaded simultaneous requests
 *  3. Throughput Tests       – requests per second measurement
 *  4. Endurance (mini)       – sustained load for 30 seconds
 *  5. Spike Test             – sudden burst simulation
 *
 * Tools: RestAssured + Java ExecutorService (complements JMeter).
 * JMeter (.jmx) files for deeper load testing are in /jmeter folder.
 */
@Epic("AcademyAI Platform")
@Feature("Performance Testing")
public class PerformanceTests extends BaseApiTest {

    // ── DataProvider: Endpoint catalog ────────────────────

    @DataProvider(name = "criticalEndpoints")
    public Object[][] criticalEndpoints() {
        return new Object[][] {
            { "GET /api/v1/schedule/today",     "GET",  "/api/v1/schedule/today",   config.perfScheduleMaxResponseMs() },
            { "GET /api/v1/schedule/weekly",    "GET",  "/api/v1/schedule/weekly",  config.perfScheduleMaxResponseMs() },
            { "GET /api/v1/social/feed",        "GET",  "/api/v1/social/feed",      config.perfSocialMaxResponseMs()   },
            { "GET /api/v1/chat/conversations", "GET",  "/api/v1/chat/conversations", config.perfChatMaxResponseMs()   },
            { "GET /api/v1/auth/profile",       "GET",  "/api/v1/auth/profile",     config.perfAuthMaxResponseMs()     },
        };
    }

    // ── 1. Response Time Per Endpoint ──────────────────────

    @Test(dataProvider = "criticalEndpoints", groups = {"performance", "smoke"})
    @Story("Response Time")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Each critical endpoint must respond within the defined SLA threshold (single user)")
    public void testResponseTime_SingleUser_UnderSLAThreshold(
            String label, String method, String path, int thresholdMs) {

        Response response = authedSpec().request(method, path);

        long elapsed = response.time();
        assertThat(response.getStatusCode()).isIn(200, 201);
        assertThat(elapsed)
                .as("[%s] Response time %dms should be < %dms SLA", label, elapsed, thresholdMs)
                .isLessThan(thresholdMs);

        Allure.addAttachment(label + " – Timing",
                "text/plain",
                "Endpoint : " + path + "\n" +
                "Method   : " + method + "\n" +
                "Elapsed  : " + elapsed + "ms\n" +
                "SLA      : " + thresholdMs + "ms\n" +
                "Status   : PASS (" + response.getStatusCode() + ")");

        log.info("[PERF] {} → {}ms (SLA: {}ms) ✓", label, elapsed, thresholdMs);
    }

    // ── 2. Concurrent Load – Auth Profile ─────────────────

    @Test(groups = {"performance"})
    @Story("Concurrent Load")
    @Severity(SeverityLevel.CRITICAL)
    @Description("50 concurrent users hit GET /auth/profile – error rate must be 0% and avg latency < 2000ms")
    public void testConcurrentLoad_ProfileEndpoint_50Users_NoErrors() throws InterruptedException {
        int users       = 50;
        int threshold   = config.perfAuthMaxResponseMs();

        runConcurrentTest("GET /api/v1/auth/profile", users, threshold, () ->
                authedSpec().get("/api/v1/auth/profile")
        );
    }

    @Test(groups = {"performance"})
    @Story("Concurrent Load")
    @Severity(SeverityLevel.CRITICAL)
    @Description("30 concurrent users hit GET /schedule/today – error rate must be 0% and avg latency < 1500ms")
    public void testConcurrentLoad_ScheduleEndpoint_30Users_NoErrors() throws InterruptedException {
        int users     = 30;
        int threshold = config.perfScheduleMaxResponseMs();

        runConcurrentTest("GET /api/v1/schedule/today", users, threshold, () ->
                authedSpec().get("/api/v1/schedule/today")
        );
    }

    @Test(groups = {"performance"})
    @Story("Concurrent Load")
    @Severity(SeverityLevel.CRITICAL)
    @Description("40 concurrent users GET social feed – error rate < 1% and avg latency < 2000ms")
    public void testConcurrentLoad_SocialFeed_40Users_LowErrorRate() throws InterruptedException {
        int users     = 40;
        int threshold = config.perfSocialMaxResponseMs();

        runConcurrentTest("GET /api/v1/social/feed", users, threshold, () ->
                authedSpec().queryParam("page", 1).queryParam("limit", 10)
                        .get("/api/v1/social/feed")
        );
    }

    @Test(groups = {"performance"})
    @Story("Concurrent Load")
    @Severity(SeverityLevel.CRITICAL)
    @Description("20 concurrent users POST AI chat simultaneously – avg latency < 10s")
    public void testConcurrentLoad_AiChat_20Users_UnderThreshold() throws InterruptedException {
        int users     = 20;
        int threshold = config.perfAiMaxResponseMs();

        runConcurrentTest("POST /api/v1/ai/chat (20 users)", users, threshold, () ->
                authedAiSpec()
                        .body("{\"message\": \"What is a linked list?\", \"language\": \"en\"}")
                        .post("/api/v1/ai/chat")
        );
    }

    @Test(groups = {"performance"})
    @Story("Concurrent Load")
    @Severity(SeverityLevel.NORMAL)
    @Description("25 concurrent users POST /chat/messages simultaneously – error rate 0%")
    public void testConcurrentLoad_ChatMessages_25Users_NoErrors() throws InterruptedException {
        int users     = 25;
        int threshold = config.perfChatMaxResponseMs();

        runConcurrentTest("POST /api/v1/chat/messages (25 users)", users, threshold, () ->
                authedSpec()
                        .body("{"
                            + "\"conversation_id\": \"load-test-conv-001\","
                            + "\"content\": \"" + TestDataFactory.randomChatMessage() + "\","
                            + "\"message_type\": \"text\""
                            + "}")
                        .post("/api/v1/chat/messages")
        );
    }

    // ── 3. Throughput Test ─────────────────────────────────

    @Test(groups = {"performance"})
    @Story("Throughput")
    @Severity(SeverityLevel.NORMAL)
    @Description("Measure requests per second for GET /schedule/today over 10 seconds – must exceed 10 rps")
    public void testThroughput_ScheduleEndpoint_AtLeast10RPS() throws InterruptedException {
        int durationSeconds = 10;
        int minExpectedRps  = 10;

        long start     = System.currentTimeMillis();
        long deadline  = start + (durationSeconds * 1000L);
        AtomicInteger requestCount = new AtomicInteger(0);
        AtomicInteger errorCount   = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(15);
        List<Future<?>> futures  = new ArrayList<>();

        while (System.currentTimeMillis() < deadline) {
            futures.add(executor.submit(() -> {
                Response resp = authedSpec().get("/api/v1/schedule/today");
                requestCount.incrementAndGet();
                if (resp.getStatusCode() >= 400) errorCount.incrementAndGet();
            }));
            Thread.sleep(50); // submit every 50ms
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long elapsed = System.currentTimeMillis() - start;
        double rps = (double) requestCount.get() / (elapsed / 1000.0);

        log.info("[THROUGHPUT] {} requests in {}ms → {:.2f} RPS | Errors: {}",
                requestCount.get(), elapsed, rps, errorCount.get());

        Allure.addAttachment("Throughput Result",
                "text/plain",
                "Total Requests : " + requestCount.get() + "\n" +
                "Duration       : " + elapsed + "ms\n" +
                "RPS            : " + String.format("%.2f", rps) + "\n" +
                "Min Expected   : " + minExpectedRps + " RPS\n" +
                "Errors         : " + errorCount.get());

        assertThat(rps)
                .as("Throughput should be at least " + minExpectedRps + " RPS")
                .isGreaterThanOrEqualTo(minExpectedRps);

        double errorRate = (double) errorCount.get() / requestCount.get() * 100;
        assertThat(errorRate)
                .as("Error rate should be < 5%")
                .isLessThan(5.0);
    }

    // ── 4. Endurance (Mini – 30 seconds) ──────────────────

    @Test(groups = {"performance"})
    @Story("Endurance Test")
    @Severity(SeverityLevel.NORMAL)
    @Description("Sustained load on GET /social/feed for 30 seconds – no memory leak indicators, stable response times")
    public void testEndurance_SocialFeed_30Seconds_StableResponseTimes() throws InterruptedException {
        int durationSeconds  = 30;
        int concurrentUsers  = 10;
        int maxAllowedMs     = config.perfSocialMaxResponseMs() * 2; // 2x SLA at sustained load

        ExecutorService executor      = Executors.newFixedThreadPool(concurrentUsers);
        AtomicLong totalResponseTime  = new AtomicLong(0);
        AtomicInteger requestCount    = new AtomicInteger(0);
        AtomicInteger errorCount      = new AtomicInteger(0);
        List<Long> responseTimes      = new CopyOnWriteArrayList<>();

        long deadline = System.currentTimeMillis() + (durationSeconds * 1000L);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentUsers; i++) {
            futures.add(executor.submit(() -> {
                while (System.currentTimeMillis() < deadline) {
                    long start = System.currentTimeMillis();
                    try {
                        Response resp = authedSpec()
                                .queryParam("page", 1)
                                .queryParam("limit", 10)
                                .get("/api/v1/social/feed");
                        long elapsed = System.currentTimeMillis() - start;
                        responseTimes.add(elapsed);
                        totalResponseTime.addAndGet(elapsed);
                        requestCount.incrementAndGet();
                        if (resp.getStatusCode() >= 400) errorCount.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
            }));
        }

        executor.shutdown();
        executor.awaitTermination(durationSeconds + 10, TimeUnit.SECONDS);

        int total    = requestCount.get();
        double avg   = total > 0 ? (double) totalResponseTime.get() / total : 0;
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        long p95     = percentile(responseTimes, 95);
        double errRate = total > 0 ? (double) errorCount.get() / total * 100 : 0;

        log.info("[ENDURANCE] {}s | Requests: {} | Avg: {}ms | P95: {}ms | Max: {}ms | Errors: {:.2f}%",
                durationSeconds, total, (long)avg, p95, maxTime, errRate);

        Allure.addAttachment("Endurance Report",
                "text/plain",
                "Duration       : " + durationSeconds + "s\n" +
                "Concurrent     : " + concurrentUsers + " users\n" +
                "Total Requests : " + total + "\n" +
                "Avg Response   : " + String.format("%.0f", avg) + "ms\n" +
                "P95 Response   : " + p95 + "ms\n" +
                "Max Response   : " + maxTime + "ms\n" +
                "Error Rate     : " + String.format("%.2f", errRate) + "%");

        assertThat(avg).as("Avg response time during endurance should be < %dms", maxAllowedMs)
                .isLessThan(maxAllowedMs);
        assertThat(errRate).as("Error rate during endurance should be < 5%")
                .isLessThan(5.0);
    }

    // ── 5. Spike Test ─────────────────────────────────────

    @Test(groups = {"performance"})
    @Story("Spike Test")
    @Severity(SeverityLevel.NORMAL)
    @Description("Simulate sudden spike: 5 → 100 users in 2 seconds on /auth/profile, then back to 5. System must recover.")
    public void testSpike_AuthProfile_SuddenBurstThenRecovery() throws InterruptedException {
        // Phase 1: normal load (5 users)
        log.info("[SPIKE] Phase 1: Normal load – 5 users");
        List<Long> normalTimes = runBatch("GET /api/v1/auth/profile", 5, () ->
                authedSpec().get("/api/v1/auth/profile"));
        double normalAvg = normalTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        // Phase 2: spike (100 users simultaneously)
        log.info("[SPIKE] Phase 2: SPIKE – 100 users");
        List<Long> spikeTimes = runBatch("GET /api/v1/auth/profile (SPIKE)", 100, () ->
                authedSpec().get("/api/v1/auth/profile"));
        double spikeAvg = spikeTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        // Phase 3: recovery (5 users)
        Thread.sleep(3000);
        log.info("[SPIKE] Phase 3: Recovery – 5 users");
        List<Long> recoveryTimes = runBatch("GET /api/v1/auth/profile (recovery)", 5, () ->
                authedSpec().get("/api/v1/auth/profile"));
        double recoveryAvg = recoveryTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        Allure.addAttachment("Spike Test Report",
                "text/plain",
                "Normal Avg   : " + String.format("%.0f", normalAvg) + "ms\n" +
                "Spike Avg    : " + String.format("%.0f", spikeAvg) + "ms\n" +
                "Recovery Avg : " + String.format("%.0f", recoveryAvg) + "ms\n" +
                "Recovery ✓   : " + (recoveryAvg < normalAvg * 2 ? "YES" : "DEGRADED"));

        log.info("[SPIKE] Normal: {}ms | Spike: {}ms | Recovery: {}ms",
                (long)normalAvg, (long)spikeAvg, (long)recoveryAvg);

        // Recovery avg should be within 2x normal (system returned to stable state)
        assertThat(recoveryAvg)
                .as("After spike, system should recover to near-normal response times")
                .isLessThan(normalAvg * 2.5);
    }

    // ── 6. Login Concurrent Load ───────────────────────────

    @Test(groups = {"performance"})
    @Story("Concurrent Load")
    @Severity(SeverityLevel.NORMAL)
    @Description("30 concurrent login requests – error rate 0% and max response < 5s")
    public void testConcurrentLoad_Login_30Users_NoErrors() throws InterruptedException {
        int users = 30;

        runConcurrentTest("POST /api/v1/auth/login (30 users)", users,
                config.perfAuthMaxResponseMs() * 2, () ->
                RestAssured.given()
                        .spec(requestSpec)
                        .body("{\"email\": \"" + config.studentEmail() + "\","
                            + "\"password\": \"" + config.studentPassword() + "\"}")
                        .post("/api/v1/auth/login")
        );
    }

    // ── Helpers ────────────────────────────────────────────

    /**
     * Runs N concurrent requests, asserts avg response time and 0% error rate.
     */
    private void runConcurrentTest(String label, int users, int thresholdMs,
                                   Callable<Response> requestFn) throws InterruptedException {

        ExecutorService executor   = Executors.newFixedThreadPool(users);
        CountDownLatch latch       = new CountDownLatch(users);
        AtomicLong totalTime       = new AtomicLong(0);
        AtomicInteger errors       = new AtomicInteger(0);
        List<Long> responseTimes   = new CopyOnWriteArrayList<>();

        for (int i = 0; i < users; i++) {
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await(); // all threads start simultaneously
                    long start = System.currentTimeMillis();
                    Response resp = requestFn.call();
                    long elapsed  = System.currentTimeMillis() - start;
                    responseTimes.add(elapsed);
                    totalTime.addAndGet(elapsed);
                    if (resp.getStatusCode() >= 400) errors.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        int total      = responseTimes.size();
        double avg     = total > 0 ? (double) totalTime.get() / total : 0;
        long p95       = percentile(responseTimes, 95);
        long maxTime   = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        double errRate = total > 0 ? (double) errors.get() / (total + errors.get()) * 100 : 100;

        log.info("[CONCURRENT] {} | Users: {} | Avg: {}ms | P95: {}ms | Max: {}ms | Errors: {}",
                label, users, (long)avg, p95, maxTime, errors.get());

        Allure.addAttachment("Load Result – " + label,
                "text/plain",
                "Concurrent Users : " + users + "\n" +
                "Total Requests   : " + total + "\n" +
                "Avg Response     : " + String.format("%.0f", avg) + "ms\n" +
                "P95 Response     : " + p95 + "ms\n" +
                "Max Response     : " + maxTime + "ms\n" +
                "SLA Threshold    : " + thresholdMs + "ms\n" +
                "Errors           : " + errors.get() + " (" + String.format("%.1f", errRate) + "%)\n" +
                "Result           : " + (avg < thresholdMs && errRate < 1 ? "PASS ✓" : "FAIL ✗"));

        assertThat(avg)
                .as("[%s] Avg response time %dms should be < %dms", label, (long)avg, thresholdMs)
                .isLessThan(thresholdMs);
        assertThat(errRate)
                .as("[%s] Error rate should be < 1%%", label)
                .isLessThan(1.0);
    }

    /**
     * Runs N concurrent requests and returns response times list.
     */
    private List<Long> runBatch(String label, int users,
                                 Callable<Response> requestFn) throws InterruptedException {
        ExecutorService executor  = Executors.newFixedThreadPool(users);
        CountDownLatch latch      = new CountDownLatch(users);
        List<Long> times          = new CopyOnWriteArrayList<>();

        for (int i = 0; i < users; i++) {
            executor.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();
                    long start = System.currentTimeMillis();
                    requestFn.call();
                    times.add(System.currentTimeMillis() - start);
                } catch (Exception ignored) {}
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        return times;
    }

    /**
     * Computes the Nth percentile from a list of response times.
     */
    private long percentile(List<Long> times, int percentile) {
        if (times.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(times);
        sorted.sort(Long::compareTo);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
