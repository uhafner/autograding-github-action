package edu.hm.hafner.grading.github;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the grading action. Starts the container and checks if the grading runs as expected.
 *
 * @author Ullrich Hafner
 */
public class GitHubAutoGradingRunnerDockerITest {
    private static final String CONFIGURATION = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "junit",
                    "name": "Unittests",
                    "pattern": "**/target/*-reports/TEST*.xml"
                  }
                ],
                "name": "JUnit",
                "passedImpact": 10,
                "skippedImpact": -1,
                "failureImpact": -5,
                "maxScore": 100
              },
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                      "id": "checkstyle",
                      "name": "CheckStyle",
                      "pattern": "**/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "PMD",
                      "pattern": "**/pmd*.xml"
                    }
                  ],
                  "errorImpact": 1,
                  "highImpact": 2,
                  "normalImpact": 3,
                  "lowImpact": 4,
                  "maxScore": 100
                },
                {
                  "name": "Bugs",
                  "id": "bugs",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "name": "SpotBugs",
                      "pattern": "**/spotbugs*.xml"
                    }
                  ],
                  "errorImpact": -11,
                  "highImpact": -12,
                  "normalImpact": -13,
                  "lowImpact": -14,
                  "maxScore": 100
                }
              ],
              "coverage": [
              {
                  "tools": [
                      {
                        "id": "jacoco",
                        "name": "Line Coverage",
                        "metric": "line",
                        "pattern": "**/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "Branch Coverage",
                        "metric": "branch",
                        "pattern": "**/jacoco.xml"
                      }
                    ],
                "name": "JaCoCo",
                "maxScore": 100,
                "coveredPercentageImpact": 1,
                "missedPercentageImpact": -1
              },
              {
                  "tools": [
                      {
                        "id": "pit",
                        "name": "Mutation Coverage",
                        "metric": "mutation",
                        "pattern": "**/mutations.xml"
                      }
                    ],
                "name": "PIT",
                "maxScore": 100,
                "coveredPercentageImpact": 1,
                "missedPercentageImpact": -1
              }
              ]
            }
            """;
    private static final String WS = "/github/workspace/target/";
    private static final String QUALITY_GATES_OK = """
            {
              "qualityGates": [
                {
                  "metric": "line",
                  "threshold": 10.0,
                  "criticality": "FAILURE"
                }
              ]
            }
            """;
    private static final String QUALITY_GATES_NOK = """
            {
              "qualityGates": [
                {
                  "metric": "line",
                  "threshold": 100.0,
                  "criticality": "FAILURE"
                }
              ]
            }
            """;

    @Test
    void shouldGradeInDockerContainer() throws TimeoutException, IOException {
        try (var container = createContainer()) {
            container.withEnv("CONFIG", CONFIGURATION);
            startContainerWithAllFiles(container);

            assertThat(readStandardOut(container))
                    .contains("Obtaining configuration from environment variable CONFIG")
                    .contains("Processing 1 test configuration(s)",
                            "-> Unittests Total: TESTS: 1",
                            "JUnit Score: 10 of 100",
                            "Processing 2 coverage configuration(s)",
                            "-> Line Coverage Total: LINE: 10.93% (33/302)",
                            "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                            "=> JaCoCo Score: 20 of 100",
                            "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                            "=> PIT Score: 16 of 100",
                            "Processing 2 static analysis configuration(s)",
                            "-> CheckStyle (checkstyle): 1 warning (normal: 1)",
                            "-> PMD (pmd): 1 warning (normal: 1)",
                            "=> Style Score: 6 of 100",
                            "-> SpotBugs (spotbugs): 1 bug (low: 1)",
                            "=> Bugs Score: 86 of 100",
                            "Autograding score - 138 of 500")
                    .contains("Environment variable 'QUALITY_GATES' not found or empty",
                            "No quality gates to evaluate",
                            "Setting conclusion to SUCCESS - all quality gates passed");
        }
    }

    @Test
    void shouldGradeWithSuccessfulQualityGate() throws TimeoutException {
        try (var container = createContainer()) {
            container.withEnv("CONFIG", CONFIGURATION).withEnv("QUALITY_GATES", QUALITY_GATES_OK);
            startContainerWithAllFiles(container);

            assertThat(readStandardOut(container))
                    .contains("Processing 1 test configuration(s)",
                            "Processing 2 coverage configuration(s)",
                            "Processing 2 static analysis configuration(s)",
                            "Autograding score - 138 of 500")
                    .contains("Found quality gates configuration in environment variable 'QUALITY_GATES'",
                            "Parsing quality gates from JSON configuration using QualityGatesConfiguration",
                            "Parsed 1 quality gate(s) from JSON configuration",
                            "Evaluating 1 quality gate(s)",
                            "Quality gates evaluation completed: ✅ SUCCESS",
                            "  Passed: 1, Failed: 0",
                            "  ✅ Line Coverage: 11.00 >= 10.00");
        }
    }

    @Test
    void shouldGradeWithFailedQualityGate() throws TimeoutException {
        try (var container = createContainer()) {
            container.withEnv("CONFIG", CONFIGURATION).withEnv("QUALITY_GATES", QUALITY_GATES_NOK);
            startContainerWithAllFiles(container);

            assertThat(readStandardOut(container))
                    .contains("Processing 1 test configuration(s)",
                            "Processing 2 coverage configuration(s)",
                            "Processing 2 static analysis configuration(s)",
                            "Autograding score - 138 of 500")
                    .contains(
                            "Autograding score - 138 of 500",
                            "Quality Gates GitHub Autograding",
                            "Found quality gates configuration in environment variable 'QUALITY_GATES'",
                            "Parsed 1 quality gate(s) from JSON configuration",
                            "Quality gates evaluation completed: ❌ FAILURE",
                            "Passed: 0, Failed: 1",
                            "❌ Line Coverage: 11.00 >= 100.00");
        }
    }

    @Test
    void shouldUseDefaultConfiguration() throws TimeoutException {
        try (var container = createContainer()) {
            startContainerWithAllFiles(container);

            assertThat(readStandardOut(container))
                    .contains(
                            "No configuration provided (environment variable CONFIG not set), using default configuration")
                    .contains("Processing 1 test configuration(s)",
                            "-> JUnit Tests Total: TESTS: 1",
                            "Tests Score: 100 of 100",
                            "Processing 2 coverage configuration(s)",
                            "-> Line Coverage Total: LINE: 10.93% (33/302)",
                            "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                            "=> Code Coverage Score: 10 of 100",
                            "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                            "-> Test Strength Total: TEST_STRENGTH: 84.62% (11/13)",
                            "=> Mutation Coverage Score: 46 of 100",
                            "Processing 2 static analysis configuration(s)",
                            "-> CheckStyle (checkstyle): 1 warning (normal: 1)",
                            "-> PMD (pmd): 1 warning (normal: 1)",
                            "=> Style Score: 98 of 100",
                            "-> SpotBugs (spotbugs): 1 bug (low: 1)",
                            "=> Bugs Score: 97 of 100",
                            "Autograding score - 351 of 500 (70%)");
        }
    }

    @Test
    void shouldShowErrors() throws TimeoutException {
        try (var container = createContainer()) {
            container.withWorkingDirectory("/github/workspace").start();
            assertThat(readStandardOut(container))
                    .contains("Processing 1 test configuration(s)",
                            "Configuration error for 'JUnit Tests'?",
                            "Tests Score: 100 of 100",
                            "Processing 2 coverage configuration(s)",
                            "=> Code Coverage Score: 100 of 100",
                            "Configuration error for 'Line Coverage'?",
                            "Configuration error for 'Branch Coverage'?",
                            "=> Mutation Coverage Score: 100 of 100",
                            "Configuration error for 'Mutation Coverage'?",
                            "Processing 2 static analysis configuration(s)",
                            "Configuration error for 'CheckStyle'?",
                            "Configuration error for 'PMD'?",
                            "Configuration error for 'SpotBugs'?",
                            "-> CheckStyle (checkstyle): No warnings",
                            "-> PMD (pmd): No warnings",
                            "=> Style Score: 100 of 100",
                            "-> SpotBugs (spotbugs): No warnings",
                            "=> Bugs Score: 100 of 100",
                            "Autograding score - 500 of 500");
        }
    }

    private GenericContainer<?> createContainer() {
        return new GenericContainer<>(DockerImageName.parse("uhafner/autograding-github-action:6.0.0-SNAPSHOT"));
    }

    private String readStandardOut(final GenericContainer<? extends GenericContainer<?>> container)
            throws TimeoutException {
        var waitingConsumer = new WaitingConsumer();
        var toStringConsumer = new ToStringConsumer();

        var composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);
        waitingConsumer.waitUntil(frame -> frame.getUtf8String().contains("End GitHub Autograding"), 60,
                TimeUnit.SECONDS);

        return toStringConsumer.toUtf8String();
    }

    private void startContainerWithAllFiles(final GenericContainer<?> container) {
        container.withWorkingDirectory("/github/workspace")
                .withCopyFileToContainer(read("checkstyle/checkstyle-result.xml"), WS + "checkstyle-result.xml")
                .withCopyFileToContainer(read("jacoco/jacoco.xml"), WS + "site/jacoco/jacoco.xml")
                .withCopyFileToContainer(read("junit/TEST-edu.hm.hafner.grading.AutoGradingActionTest.xml"),
                        WS + "surefire-reports/TEST-Aufgabe3Test.xml")
                .withCopyFileToContainer(read("pit/mutations.xml"), WS + "pit-reports/mutations.xml")
                .withCopyFileToContainer(read("pmd/pmd.xml"), WS + "pmd.xml")
                .withCopyFileToContainer(read("spotbugs/spotbugsXml.xml"), WS + "spotbugsXml.xml")
                .start();
    }

    private MountableFile read(final String resourceName) {
        return MountableFile.forClasspathResource("/" + resourceName);
    }
}
