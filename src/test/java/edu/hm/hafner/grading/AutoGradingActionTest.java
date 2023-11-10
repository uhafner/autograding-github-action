package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

class AutoGradingActionTest {
    @Test
    void shouldRunAnalysisWithMultipleTools() {
        var configuration = """
                {
                  "analysis": {
                    "name": "Checkstyle and SpotBugs",
                    "tools": [
                      {
                        "id": "checkstyle",
                        "pattern": "**/target/**/checkstyle*.xml"
                      },
                      {
                        "id": "spotbugs",
                        "pattern": "**/target/**/spotbugsXml.xml"
                      }
                    ],
                    "errorImpact": -1,
                    "highImpact": -2,
                    "normalImpact": -3,
                    "lowImpact": -4,
                    "maxScore": 50
                  }
                }
                """;

        var log = new FilteredLog("Test Errors");
        var score = new AggregatedScore(configuration, log);

        var logHandler = new LogHandler(System.out, log);

        score.gradeAnalysis(new ConsoleAnalysisReportFactory());

        assertThat(log.getInfoMessages()).contains(
                "Searching for CheckStyle results matching file name pattern **/target/**/checkstyle*.xml",
                "Searching for SpotBugs results matching file name pattern **/target/**/spotbugsXml.xml",
                "- ./target/test-classes/checkstyle/checkstyle-ignores.xml: 18 warnings",
                "- ./target/test-classes/checkstyle/checkstyle-result.xml: 3 warnings",
                "-> CheckStyle Total: 21 warnings",
                "- ./target/test-classes/findbugs/spotbugsXml.xml: 9 warnings",
                "-> SpotBugs Total: 9 warnings",
                "=> Checkstyle and SpotBugs Score: 0 of 50");

        logHandler.print();
    }

    @Test
    void shouldRunAnalysisWithSingleTool() {
        var configuration = """
                {
                  "analysis": {
                    "tools": [
                      {
                        "id": "spotbugs",
                        "pattern": "**/target/**/spotbugsXml.xml"
                      }
                    ],
                    "errorImpact": -1,
                    "highImpact": -1,
                    "normalImpact": -1,
                    "lowImpact": -1,
                    "maxScore": 50
                  }
                }
                """;

        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(configuration, log);

        var logHandler = new LogHandler(System.out, log);

        score.gradeAnalysis(new ConsoleAnalysisReportFactory());

        assertThat(log.getInfoMessages()).contains(
                "Searching for SpotBugs results matching file name pattern **/target/**/spotbugsXml.xml",
                "- ./target/test-classes/findbugs/spotbugsXml.xml: 9 warnings",
                "-> SpotBugs Total: 9 warnings",
                "=> Static Analysis Warnings Score: 41 of 50");

        logHandler.print();
    }

    @Test
    void shouldRunCoverageWithSingleTool() {
        var configuration = """
                {
                  "coverage": {
                    "tools": [
                      {
                        "id": "jacoco",
                        "metric": "line",
                        "pattern": "**/target/**/jacoco.xml"
                      }
                    ],
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """;

        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(configuration, log);

        var logHandler = new LogHandler(System.out, log);

        score.gradeCoverage(new ConsoleCoverageReportFactory());

        assertThat(StringUtils.join(log.getInfoMessages())).contains(
                "Searching for jacoco results matching file name pattern **/target/**/jacoco.xml",
                "- ./target/test-classes/jacoco/jacoco.xml",
                "LINE: 87.99% (315/358)",
                "=> Code Coverage Score: 76 of 100");

        logHandler.print();
    }

    @Test
    void shouldRunCoverageWithMultipleTools() {
        var configuration = """
                {
                  "coverage": {
                    "tools": [
                      {
                        "id": "jacoco",
                        "metric": "line",
                        "pattern": "**/target/**/jacoco.xml"
                      },
                      {
                        "id": "jacoco",
                        "metric": "branch",
                        "pattern": "**/target/**/jacoco.xml"
                      }
                    ],
                    "maxScore": 100,
                    "coveredPercentageImpact": 1,
                    "missedPercentageImpact": -1
                  }
                }
                """;

        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(configuration, log);

        var logHandler = new LogHandler(System.out, log);

        score.gradeCoverage(new ConsoleCoverageReportFactory());

        assertThat(StringUtils.join(log.getInfoMessages())).contains(
                "Searching for jacoco results matching file name pattern **/target/**/jacoco.xml",
                "- ./target/test-classes/jacoco/jacoco.xml",
                "LINE: 87.99% (315/358)",
                "BRANCH: 61.54% (16/26)",
                "=> Code Coverage Score: 50 of 100");

        logHandler.print();
    }

    @Test
    void shouldGradeTests() {
        var configuration = """
                {
                  "tests": {
                    "tools": [
                      {
                        "id": "junit",
                        "name": "JUnit Tests",
                        "pattern": "**/target/**/junit/*.xml"
                      }
                    ],
                    "name": "JUnit",
                    "passedImpact": 0,
                    "skippedImpact": -1,
                    "failureImpact": -5,
                    "maxScore": 100
                  }
                }
                """;

        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(configuration, log);

        var logHandler = new LogHandler(System.out, log);

        score.gradeTests(new ConsoleTestReportFactory());

        assertThat(StringUtils.join(log.getInfoMessages())).contains(
                "Searching for JUnit results matching file name pattern **/target/**/junit/*.xml",
                "- ./target/test-classes/junit/TEST-Aufgabe3Test.xml",
                "- ./target/test-classes/junit/TEST-de.tobiasmichael.me.MyTest-Passed.xml",
                "- ./target/test-classes/junit/TEST-de.tobiasmichael.me.MyTest-Not-Passed.xml",
                "-> JUnit Tests Total: 41 tests (28 passed, 13 failed, 0 skipped)",
                "=> JUnit Score: 35 of 100");

        logHandler.print();
    }
}
