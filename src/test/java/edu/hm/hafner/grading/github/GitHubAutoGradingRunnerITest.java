package edu.hm.hafner.grading.github;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.ResourceTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for the grading action. Starts the container and checks if the grading runs as expected.
 *
 * @author Ullrich Hafner
 */
public class GitHubAutoGradingRunnerITest extends ResourceTest {
    private static final String CONFIGURATION = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "test",
                    "name": "Unittests",
                    "pattern": "**/src/**/TEST*.xml"
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
                      "pattern": "**/src/**/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "PMD",
                      "pattern": "**/src/**/pmd*.xml"
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
                      "pattern": "**/src/**/spotbugs*.xml"
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
                        "pattern": "**/src/**/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "Branch Coverage",
                        "metric": "branch",
                        "pattern": "**/src/**/jacoco.xml"
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
                        "pattern": "**/src/**/mutations.xml"
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

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION)
    void shouldGradeWithConfigurationFromEnvironment() {
        assertThat(runAutoGrading())
                .contains("Obtaining configuration from environment variable CONFIG")
                .contains(new String[] {
                        "Processing 1 test configuration(s)",
                        "-> Unittests Total: TESTS: 37 tests",
                        "JUnit Score: 100 of 100",
                        "Processing 2 coverage configuration(s)",
                        "-> Line Coverage Total: LINE: 10.93% (33/302)",
                        "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                        "=> JaCoCo Score: 20 of 100",
                        "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                        "=> PIT Score: 16 of 100",
                        "Processing 2 static analysis configuration(s)",
                        "-> CheckStyle Total: 19 warnings",
                        "-> PMD Total: 41 warnings",
                        "=> Style Score: 100 of 100",
                        "-> SpotBugs Total: 1 warnings",
                        "=> Bugs Score: 86 of 100",
                        "Total score - 322 of 500 (unit tests: 100/100, code coverage: 20/100, mutation coverage: 16/100, analysis: 186/200)"});
    }

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION)
    void shouldCreateAnnotations() {
        var outputStream = new ByteArrayOutputStream();
        var printStream = new PrintStream(outputStream);
        var runner = new GitHubAutoGradingRunner(printStream);
        runner.run();
        var score = Objects.requireNonNull(runner.getAggregation());

        assertThat(new AnnotationBuilder().createAnnotations(score, new FilteredLog("errors"))).extracting("message")
                .containsOnly(
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Vor ',' befindet sich ein Leerzeichen.",
                        "Es fehlt ein Javadoc-Kommentar.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "Consider using varargs for methods or constructors which take an array the last parameter.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "The method 'main(String...)' has a cognitive complexity of 16, current threshold is 15.",
                        "These nested if statements could be combined.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "Avoid using Literals in Conditional Statements.",
                        "Consider using varargs for methods or constructors which take an array the last parameter.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "This abstract class does not have any abstract methods.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "This abstract class does not have any abstract methods.",
                        "Lines 15-27 are not covered by tests",
                        "Lines 62-79 are not covered by tests",
                        "Lines 102-103 are not covered by tests",
                        "Lines 23-49 are not covered by tests",
                        "Lines 13-15 are not covered by tests",
                        "Lines 19-68 are not covered by tests",
                        "Lines 16-27 are not covered by tests",
                        "Lines 41-140 are not covered by tests",
                        "Lines 152-153 are not covered by tests",
                        "Line 160 is not covered by tests",
                        "Lines 164-166 are not covered by tests",
                        "Lines 17-32 are not covered by tests",
                        "Lines 40-258 are not covered by tests",
                        "Line 146 is only partially covered, one branch is missing",
                        "Line 159 is only partially covered, one branch is missing",
                        "One mutation survived in line 147 (VoidMethodCallMutator)",
                        "One mutation survived in line 29 (EmptyObjectReturnValsMutator)",
                        "Usage of GetResource in edu.hm.hafner.grading.AutoGradingAction.readDefaultConfiguration() may be unsafe if class is extended");
    }

    private static final String CONFIGURATION_WRONG_PATHS = """
            {
              "tests": {
                "tools": [
                  {
                    "id": "test",
                    "name": "Unittests",
                    "pattern": "**/does-not-exist/TEST*.xml"
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
                      "pattern": "**/does-not-exist/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "PMD",
                      "pattern": "**/does-not-exist/pmd*.xml"
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
                      "pattern": "**/does-not-exist/spotbugs*.xml"
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
                        "pattern": "**/does-not-exist/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "name": "Branch Coverage",
                        "metric": "branch",
                        "pattern": "**/does-not-exist/jacoco.xml"
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
                        "pattern": "**/does-not-exist/mutations.xml"
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

    @Test
    @SetEnvironmentVariable(key = "CONFIG", value = CONFIGURATION_WRONG_PATHS)
    void shouldShowErrors() {
        assertThat(runAutoGrading())
                .contains(new String[] {
                        "Processing 1 test configuration(s)",
                        "-> Unittests Total: TESTS: 0 tests",
                        "Configuration error for 'Unittests'?",
                        "JUnit Score: 100 of 100",
                        "Processing 2 coverage configuration(s)",
                        "=> JaCoCo Score: 0 of 100",
                        "Configuration error for 'Line Coverage'?",
                        "Configuration error for 'Branch Coverage'?",
                        "=> PIT Score: 0 of 100",
                        "Configuration error for 'Mutation Coverage'?",
                        "Processing 2 static analysis configuration(s)",
                        "Configuration error for 'CheckStyle'?",
                        "Configuration error for 'PMD'?",
                        "Configuration error for 'SpotBugs'?",
                        "-> CheckStyle Total: 0 warnings",
                        "-> PMD Total: 0 warnings",
                        "=> Style Score: 0 of 100",
                        "-> SpotBugs Total: 0 warnings",
                        "=> Bugs Score: 100 of 100",
                        "Total score - 200 of 500 (unit tests: 100/100, code coverage: 0/100, mutation coverage: 0/100, analysis: 100/200)"});
    }

    private String runAutoGrading() {
        var outputStream = new ByteArrayOutputStream();
        var runner = new GitHubAutoGradingRunner(new PrintStream(outputStream));
        runner.run();
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}