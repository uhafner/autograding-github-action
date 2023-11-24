package edu.hm.hafner.grading;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
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
public class AutoGradingActionITest {
    private static final String CONFIGURATION = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "test",
                    "name": "Unittests",
                    "pattern": "**/junit*.xml"
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
    private static final String WS = "/github/workspace/";
    private static final String[] DEFAULT_CONFIG_OUTPUT = {
            "Processing 1 test configuration(s)",
            "-> Unittests Total: TESTS: 33 tests",
            "JUnit Score: 100 of 100",
            "Processing 2 coverage configuration(s)",
            "-> Line Coverage Total: LINE: 87.99% (315/358)",
            "-> Branch Coverage Total: BRANCH: 61.54% (16/26)",
            "=> JaCoCo Score: 50 of 100",
            "-> Mutation Coverage Total: MUTATION: 72.77% (139/191)",
            "=> PIT Score: 46 of 100",
            "Processing 2 static analysis configuration(s)",
            "-> CheckStyle Total: 3 warnings",
            "-> PMD Total: 5 warnings",
            "=> Style Score: 23 of 100",
            "-> SpotBugs Total: 9 warnings",
            "=> Bugs Score: 0 of 100",
            "Total score: 219/500 (unit tests: 100/100, code coverage: 50/100, mutation coverage: 46/100, analysis: 23/200)"};

    @Test
    void shouldGradeInDockerContainer() throws TimeoutException {
        try (var container = createContainer()) {
            container.withEnv("CONFIG", CONFIGURATION);
            startContainerWithAllFiles(container);

            assertThat(readStandardOut(container))
                    .contains("Obtaining configuration from environment variable CONFIG")
                    .contains(DEFAULT_CONFIG_OUTPUT);
        }

    }

    private GenericContainer<?> createContainer() {
        return new GenericContainer<>(DockerImageName.parse("uhafner/autograding-github-action:2.0.0-alpha"));
    }

    @Test
    void shouldUseDefaultConfiguration() throws TimeoutException {
        try (var container = createContainer()) {
            startContainerWithAllFiles(container);

            assertThat(readStandardOut(container))
                    .contains("No configuration provided (environment variable CONFIG not set), using default configuration")
                    .contains(DEFAULT_CONFIG_OUTPUT);
        }

    }

    private String readStandardOut(final GenericContainer<? extends GenericContainer<?>> container) throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        ToStringConsumer toStringConsumer = new ToStringConsumer();

        Consumer<OutputFrame> composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);
        waitingConsumer.waitUntil(frame ->
                frame.getUtf8String().contains("at edu.hm.hafner.grading.AutoGradingAction.main(AutoGradingAction.java")
                        || frame.getUtf8String().contains("End Grading"), 60, TimeUnit.SECONDS);

        return toStringConsumer.toUtf8String();
    }

    private void startContainerWithAllFiles(final GenericContainer<?> container) {
        container.withWorkingDirectory("/github/workspace")
                .withCopyFileToContainer(read("checkstyle/checkstyle-result.xml"), WS + "checkstyle.xml")
                .withCopyFileToContainer(read("jacoco/jacoco.xml"), WS + "jacoco.xml")
                .withCopyFileToContainer(read("junit/TEST-Aufgabe3Test.xml"), WS + "junit.xml")
                .withCopyFileToContainer(read("pit/mutations.xml"), WS + "mutations.xml")
                .withCopyFileToContainer(read("pmd/pmd.xml"), WS + "pmd.xml")
                .withCopyFileToContainer(read("spotbugs/spotbugsXml.xml"), WS + "spotbugs.xml")
                .start();
    }

    private MountableFile read(final String resourceName) {
        return MountableFile.forClasspathResource("/" + resourceName);
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = "{}")
    void shouldReadConfigurationFromEnvironment() {
        var action = new AutoGradingAction();

        assertThat(action.getConfiguration()).isEqualTo("{}");
    }
}
