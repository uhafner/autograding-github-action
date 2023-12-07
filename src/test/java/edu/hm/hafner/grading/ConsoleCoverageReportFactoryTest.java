package edu.hm.hafner.grading;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

class ConsoleCoverageReportFactoryTest {
    private static final String CONFIGURATION = """
            {
              "coverage": {
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
              }
            }
            """;

    @Test
    void shouldCreateSingleReport() {
        var log = new FilteredLog("Errors");
        var jacoco = new ToolConfiguration("jacoco", "Coverage",
                "**/src/**/jacoco.xml", "", Metric.LINE.name());

        var factory = new ConsoleCoverageReportFactory();

        var node = factory.create(jacoco, log);

        assertFileNodes(node.getAllFileNodes());
        assertThat(log.getInfoMessages()).containsExactly(
                "Searching for Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- ./src/test/resources/jacoco/jacoco.xml: LINE: 10.93% (33/302)",
                "-> Coverage Total: LINE: 10.93% (33/302)");
    }

    @Test
    void shouldCreateAggregation() {
        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(CONFIGURATION, log);

        score.gradeCoverage(new ConsoleCoverageReportFactory());

        assertFileNodes(score.getCoveredFiles(Metric.LINE));
        assertThat(log.getInfoMessages()).contains(
                "Searching for Line Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- ./src/test/resources/jacoco/jacoco.xml: LINE: 10.93% (33/302)",
                "-> Line Coverage Total: LINE: 10.93% (33/302)",
                "Searching for Branch Coverage results matching file name pattern **/src/**/jacoco.xml",
                "- ./src/test/resources/jacoco/jacoco.xml: BRANCH: 9.52% (4/42)",
                "-> Branch Coverage Total: BRANCH: 9.52% (4/42)",
                "=> JaCoCo Score: 20 of 100");
    }

    private void assertFileNodes(final List<FileNode> fileNodes) {
        assertThat(fileNodes).extracting(FileNode::getName).containsExactly("ReportFactory.java",
                "ReportFinder.java",
                "ConsoleCoverageReportFactory.java",
                "FileNameRenderer.java",
                "LogHandler.java",
                "ConsoleTestReportFactory.java",
                "AutoGradingAction.java",
                "ConsoleAnalysisReportFactory.java",
                "GitHubPullRequestWriter.java");
    }
}
