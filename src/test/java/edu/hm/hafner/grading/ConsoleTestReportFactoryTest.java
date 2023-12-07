package edu.hm.hafner.grading;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

class ConsoleTestReportFactoryTest {
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
              }
            }
            """;

    @Test
    void shouldCreateSingleReport() {
        var factory = new ConsoleTestReportFactory();
        var log = new FilteredLog("Errors");
        var node = factory.create(new ToolConfiguration("junit", "Tests",
                "**/src/**/TEST*.xml", "", Metric.TESTS.name()), log);
        assertTestClasses(node.getAllClassNodes());
        assertThat(log.getInfoMessages()).containsExactly(
                "Searching for Tests results matching file name pattern **/src/**/TEST*.xml",
                "- ./src/test/resources/junit/TEST-Aufgabe3Test.xml: TESTS: 33",
                "- ./src/test/resources/junit/TEST-edu.hm.hafner.grading.AutoGradingActionTest.xml: TESTS: 1",
                "- ./src/test/resources/junit/TEST-edu.hm.hafner.grading.ReportFinderTest.xml: TESTS: 3",
                "-> Tests Total: TESTS: 37",
                "-> Tests Total: TESTS: 37 tests");
    }

    private void assertTestClasses(final List<ClassNode> classNodes) {
        assertThat(classNodes).extracting(ClassNode::getName).containsExactly(
                "Aufgabe3Test",
                "edu.hm.hafner.grading.AutoGradingActionTest",
                "edu.hm.hafner.grading.ReportFinderTest");
    }

    @Test
    void shouldCreateAggregation() {
        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(CONFIGURATION, log);

        score.gradeTests(new ConsoleTestReportFactory());

        assertTestClasses(score.getTestScores().get(0).getReport().getAllClassNodes());
        assertThat(log.getInfoMessages()).contains(
                "Searching for Unittests results matching file name pattern **/src/**/TEST*.xml",
                "- ./src/test/resources/junit/TEST-Aufgabe3Test.xml: TESTS: 33",
                "- ./src/test/resources/junit/TEST-edu.hm.hafner.grading.AutoGradingActionTest.xml: TESTS: 1",
                "- ./src/test/resources/junit/TEST-edu.hm.hafner.grading.ReportFinderTest.xml: TESTS: 3",
                "-> Unittests Total: TESTS: 37 tests",
                "=> JUnit Score: 100 of 100");
    }
}
