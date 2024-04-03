package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

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
                    "id": "test",
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
    private static final String LOCAL_METRICS_FILE = "target/metrics.env";

    @Test
    void shouldGradeInDockerContainer() throws TimeoutException, IOException {
        try (var container = createContainer()) {
            container.withEnv("CONFIG", CONFIGURATION);
            startContainerWithAllFiles(container);

            var metrics = new String[] {
                    "tests=1",
                    "line=11",
                    "branch=10",
                    "mutation=8",
                    "bugs=1",
                    "spotbugs=1",
                    "style=2",
                    "pmd=1",
                    "checkstyle=1"};

            assertThat(readStandardOut(container))
                    .contains("Obtaining configuration from environment variable CONFIG")
                    .contains(metrics)
                    .contains(new String[] {
                            "Processing 1 test configuration(s)",
                            "-> Unittests Total: TESTS: 1 tests",
                            "JUnit Score: 10 of 100",
                            "Processing 2 coverage configuration(s)",
                            "-> Line Coverage Total: LINE: 10.93% (33/302)",
                            "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                            "=> JaCoCo Score: 20 of 100",
                            "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                            "=> PIT Score: 16 of 100",
                            "Processing 2 static analysis configuration(s)",
                            "-> CheckStyle Total: 1 warnings",
                            "-> PMD Total: 1 warnings",
                            "=> Style Score: 6 of 100",
                            "-> SpotBugs Total: 1 warnings",
                            "=> Bugs Score: 86 of 100",
                            "Autograding score - 138 of 500"});

            container.copyFileFromContainer("/github/workspace/metrics.env", LOCAL_METRICS_FILE);
            assertThat(Files.readString(Path.of(LOCAL_METRICS_FILE)))
                    .contains(metrics);
        }
    }

    @Test
    void shouldUseDefaultConfiguration() throws TimeoutException {
        try (var container = createContainer()) {
            startContainerWithAllFiles(container);

            assertThat(readStandardOut(container))
                    .contains("No configuration provided (environment variable CONFIG not set), using default configuration")
                    .contains(new String[] {
                            "Processing 1 test configuration(s)",
                            "-> Tests Total: TESTS: 1 tests",
                            "Tests Score: 100 of 100",
                            "Processing 2 coverage configuration(s)",
                            "-> Line Coverage Total: LINE: 10.93% (33/302)",
                            "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                            "=> Code Coverage Score: 10 of 100",
                            "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                            "=> Mutation Coverage Score: 8 of 100",
                            "Processing 2 static analysis configuration(s)",
                            "-> CheckStyle Total: 1 warnings",
                            "-> PMD Total: 1 warnings",
                            "=> Style Score: 98 of 100",
                            "-> SpotBugs Total: 1 warnings",
                            "=> Bugs Score: 97 of 100",
                            "Autograding score - 313 of 500"});
        }
    }

    @Test
    void shouldShowErrors() throws TimeoutException {
        try (var container = createContainer()) {
            container.withWorkingDirectory("/github/workspace").start();
            assertThat(readStandardOut(container))
                    .contains(new String[] {
                            "Processing 1 test configuration(s)",
                            "-> Tests Total: TESTS: 0 tests",
                            "Configuration error for 'Tests'?",
                            "Tests Score: 100 of 100",
                            "Processing 2 coverage configuration(s)",
                            "=> Code Coverage Score: 0 of 100",
                            "Configuration error for 'Line Coverage'?",
                            "Configuration error for 'Branch Coverage'?",
                            "=> Mutation Coverage Score: 0 of 100",
                            "Configuration error for 'Mutation Coverage'?",
                            "Processing 2 static analysis configuration(s)",
                            "Configuration error for 'CheckStyle'?",
                            "Configuration error for 'PMD'?",
                            "Configuration error for 'SpotBugs'?",
                            "-> CheckStyle Total: 0 warnings",
                            "-> PMD Total: 0 warnings",
                            "=> Style Score: 100 of 100",
                            "-> SpotBugs Total: 0 warnings",
                            "=> Bugs Score: 100 of 100",
                            "Autograding score - 300 of 500"});
        }
    }

    private GenericContainer<?> createContainer() {
        return new GenericContainer<>(DockerImageName.parse("uhafner/autograding-github-action:3.19.0"));
    }

    private String readStandardOut(final GenericContainer<? extends GenericContainer<?>> container) throws TimeoutException {
        var waitingConsumer = new WaitingConsumer();
        var toStringConsumer = new ToStringConsumer();

        var composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);
        waitingConsumer.waitUntil(frame -> frame.getUtf8String().contains("End Autograding"), 60, TimeUnit.SECONDS);

        return toStringConsumer.toUtf8String();
    }

    private void startContainerWithAllFiles(final GenericContainer<?> container) {
        container.withWorkingDirectory("/github/workspace")
                .withCopyFileToContainer(read("checkstyle/checkstyle-result.xml"), WS + "checkstyle-result.xml")
                .withCopyFileToContainer(read("jacoco/jacoco.xml"), WS + "site/jacoco/jacoco.xml")
                .withCopyFileToContainer(read("junit/TEST-edu.hm.hafner.grading.AutoGradingActionTest.xml"), WS + "surefire-reports/TEST-Aufgabe3Test.xml")
                .withCopyFileToContainer(read("pit/mutations.xml"), WS + "pit-reports/mutations.xml")
                .withCopyFileToContainer(read("pmd/pmd.xml"), WS + "pmd.xml")
                .withCopyFileToContainer(read("spotbugs/spotbugsXml.xml"), WS + "spotbugsXml.xml")
                .start();
    }

    private MountableFile read(final String resourceName) {
        return MountableFile.forClasspathResource("/" + resourceName);
    }
}
