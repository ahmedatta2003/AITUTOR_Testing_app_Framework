package com.academyai.listeners;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TestNG listener that logs test lifecycle events and attaches
 * metadata to Allure reports for the AcademyAI test suite.
 */
public class AllureTestListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(AllureTestListener.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void onStart(ITestContext context) {
        log.info("═══════════════════════════════════════════════════════");
        log.info("  AcademyAI API Test Suite Started: {}", context.getName());
        log.info("  Time: {}", LocalDateTime.now().format(FMT));
        log.info("═══════════════════════════════════════════════════════");

        Allure.description("**AcademyAI – Academic Platform API Test Suite**\n\n" +
                "Testing REST APIs for: Auth • AI Chat • Social Feed • " +
                "Lecture Schedule • Student Chat\n\n" +
                "Stack: Rest Assured | TestNG | Allure | Maven");
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info("▶ [START] {}.{}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("✓ [PASS]  {}.{} ({} ms)",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getEndMillis() - result.getStartMillis());

        Allure.addAttachment("Test Duration",
                "text/plain",
                "Execution time: " + (result.getEndMillis() - result.getStartMillis()) + " ms");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        log.error("✗ [FAIL]  {}.{} – {}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getThrowable().getMessage());

        Allure.addAttachment("Failure Reason",
                "text/plain",
                result.getThrowable() != null ? result.getThrowable().toString() : "Unknown");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("⊘ [SKIP]  {}.{}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName());
    }

    @Override
    public void onFinish(ITestContext context) {
        int total   = context.getAllTestMethods().length;
        int passed  = context.getPassedTests().size();
        int failed  = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();

        log.info("═══════════════════════════════════════════════════════");
        log.info("  Suite Finished: {}", context.getName());
        log.info("  Total: {} | Passed: {} | Failed: {} | Skipped: {}",
                total, passed, failed, skipped);
        log.info("  Time: {}", LocalDateTime.now().format(FMT));
        log.info("═══════════════════════════════════════════════════════");
    }
}
