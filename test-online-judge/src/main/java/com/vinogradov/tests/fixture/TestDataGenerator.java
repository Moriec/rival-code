package com.vinogradov.tests.fixture;

import com.vinogradov.contracts.submissionResult.enums.JudgeStatus;
import com.vinogradov.contracts.submissions.enums.ProgrammingLanguages;
import com.vinogradov.contracts.submissions.model.ComputingTask;
import com.vinogradov.contracts.submissions.model.TestCase;
import com.vinogradov.contracts.submissions.model.UserCode;

import java.util.List;
import java.util.UUID;

public class TestDataGenerator {

    public static TestData generateAcceptedJava() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"42\");\n" +
                "    }\n" +
                "}";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.JAVA)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("42").build()
                ))
                .timeLimitMs(2000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.ACCEPTED);
    }

    public static TestData generateAcceptedCpp() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "#include <iostream>\n" +
                "int main() {\n" +
                "    std::cout << 42 << std::endl;\n" +
                "    return 0;\n" +
                "}";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.CPP)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("42").build()
                ))
                .timeLimitMs(2000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.ACCEPTED);
    }

    public static TestData generateCompilationError() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"missing semicolon\")\n" +
                "    }\n" +
                "}";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.JAVA)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("anything").build()
                ))
                .timeLimitMs(2000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.COMPILATION_ERROR);
    }

    public static TestData generateWrongAnswer() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"43\");\n" +
                "    }\n" +
                "}";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.JAVA)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("42").build()
                ))
                .timeLimitMs(2000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.WRONG_ANSWER);
    }

    public static TestData generateRuntimeError() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        int[] arr = new int[0];\n" +
                "        System.out.println(arr[0]);\n" +
                "    }\n" +
                "}";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.JAVA)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("anything").build()
                ))
                .timeLimitMs(2000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.RUNTIME_ERROR);
    }

    public static TestData generateTimeLimit() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        while(true) {}\n" +
                "    }\n" +
                "}";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.JAVA)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("anything").build()
                ))
                .timeLimitMs(1000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.TIME_LIMIT_EXCEEDED);
    }

    public static TestData generateMemoryLimit() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "#include <vector>\n" +
                "#include <iostream>\n" +
                "int main() {\n" +
                "    std::vector<int> huge;\n" +
                "    while(true) { huge.push_back(1); }\n" +
                "    return 0;\n" +
                "}";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.CPP)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("anything").build()
                ))
                .timeLimitMs(5000L)
                .memoryLimitKb(10000L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.MEMORY_LIMIT_EXCEEDED);
    }

    public static TestData generateAcceptedPython() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "print(42)";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.PYTHON)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("42").build()
                ))
                .timeLimitMs(5000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.ACCEPTED);
    }

    public static TestData generateAcceptedRust() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "fn main() { println!(\"42\"); }";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.RUST)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("42").build()
                ))
                .timeLimitMs(5000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.ACCEPTED);
    }

    public static TestData generateAcceptedRuby() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "puts 42";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.RUBY)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("42").build()
                ))
                .timeLimitMs(5000000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.ACCEPTED);
    }

    public static TestData generateAcceptedJavaScript() {
        String submissionId = "test-" + UUID.randomUUID().toString().substring(0, 8);
        String code = "console.log(42)";

        ComputingTask task = ComputingTask.builder()
                .submissionId(submissionId)
                .userCode(UserCode.builder()
                        .sourceCode(code)
                        .language(ProgrammingLanguages.JAVASCRIPT)
                        .build())
                .testCases(List.of(
                        TestCase.builder().input("").expectedOutput("42").build()
                ))
                .timeLimitMs(5000L)
                .memoryLimitKb(65536L)
                .build();

        return new TestData(submissionId, task, JudgeStatus.ACCEPTED);
    }
}
