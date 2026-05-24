package com.rivalcode.tests.runner;

import com.rivalcode.contracts.submissionResult.model.JudgeResult;
import com.rivalcode.tests.validator.ValidationResult;
import com.rivalcode.tests.fixture.TestData;
import com.rivalcode.tests.fixture.TestDataGenerator;
import com.rivalcode.tests.service.TestSubmissionService;
import com.rivalcode.tests.validator.ResultValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class TestRunner {

    private final TestSubmissionService submissionService;
    private final ResultValidator validator;

    private final AtomicInteger totalTests = new AtomicInteger(0);
    private final AtomicInteger passedTests = new AtomicInteger(0);
    private final AtomicLong lastRunTime = new AtomicLong(0);

    @Scheduled(fixedDelayString = "${app.test.interval-ms:300000}", initialDelayString = "${app.test.initial-delay-ms:10000}")
    public void runTests() {
        LocalDateTime now = LocalDateTime.now();
        log.info("========== Starting test run at {} ==========", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        List<TestData> testCases = generateTestCases();
        int passed = 0;
        int failed = 0;

        int id = 0;
        log.info("Количество тестов: {}", testCases.size());
        for (TestData testData : testCases) {
            try {
                log.info("Running test: {} (expecting {})", testData.getSubmissionId(), testData.getExpectedStatus());
                log.info("Запускается тест {} - {}", id, testData.getTask().getUserCode().getLanguage());
                if(testData.getTask().getUserCode().getSourceCode().equals("puts 42")){
                    log.info("ruby запускается");
                }
                id++;
                JudgeResult result = submissionService.submitAndWaitForResult(testData.getTask());
                log.info("Пришел результат");
                if (result == null) {
                    log.error("FAILED - No result received for test: {}", testData.getSubmissionId());
                    failed++;
                    continue;
                }
                log.info("Результат не налл");

                ValidationResult validation = validator.validate(result, testData);

                log.info("Прошла валидация");
                if (validator.isPass(validation)) {
                    log.info("PASSED - {}", validation.getMessage());
                    passed++;
                } else {
                    log.error("FAILED - {}", validation.getMessage());
                    failed++;
                }

            } catch (Exception e) {
                log.error("ERROR - Exception during test execution for {}", testData.getSubmissionId(), e);
                failed++;
            }
        }

        log.info("Конец цикла");
        int total = testCases.size();
        totalTests.set(total);
        passedTests.set(passed);
        lastRunTime.set(System.currentTimeMillis());

        log.info("========== Test run completed ==========");
        log.info("Total: {}, Passed: {}, Failed: {}", total, passed, failed);
        double passRate = total > 0 ? (100.0 * passed) / total : 0;
        log.info("Pass rate: %.2f%%".formatted(passRate));
        log.info("==========================================");
    }

    private List<TestData> generateTestCases() {
        List<TestData> testCases = new ArrayList<>();

        testCases.add(TestDataGenerator.generateAcceptedJava());
        testCases.add(TestDataGenerator.generateAcceptedCpp());
        testCases.add(TestDataGenerator.generateAcceptedPython());
        testCases.add(TestDataGenerator.generateAcceptedRust());
        testCases.add(TestDataGenerator.generateAcceptedRuby());
        testCases.add(TestDataGenerator.generateAcceptedJavaScript());
        testCases.add(TestDataGenerator.generateCompilationError());
        testCases.add(TestDataGenerator.generateWrongAnswer());
        testCases.add(TestDataGenerator.generateRuntimeError());
        testCases.add(TestDataGenerator.generateTimeLimit());
        testCases.add(TestDataGenerator.generateMemoryLimit());

        return testCases;
    }

    public int getTotalTests() {
        return totalTests.get();
    }

    public int getPassedTests() {
        return passedTests.get();
    }

    public long getLastRunTime() {
        return lastRunTime.get();
    }

    public String getTestStatus() {
        int total = totalTests.get();
        if (total == 0) {
            return "No tests run yet";
        }
        int passed = passedTests.get();
        return String.format("Total: %d, Passed: %d, Failed: %d, Rate: %.2f%%",
                total, passed, total - passed, (100.0 * passed) / total);
    }
}
