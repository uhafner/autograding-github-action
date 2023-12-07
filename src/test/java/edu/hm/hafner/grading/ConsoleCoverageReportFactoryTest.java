package edu.hm.hafner.grading;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;
import edu.hm.hafner.util.FilteredLog;

import org.kohsuke.github.GHCheckRunBuilder.Annotation;
import org.kohsuke.github.GHCheckRunBuilder.Output;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConsoleCoverageReportFactoryTest {
    private static final String CONFIGURATION = """
            {
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
                "=> JaCoCo Score: 20 of 100",
                "Searching for Mutation Coverage results matching file name pattern **/src/**/mutations.xml",
                "- ./src/test/resources/pit/mutations.xml: MUTATION: 7.86% (11/140)",
                "-> Mutation Coverage Total: MUTATION: 7.86% (11/140)",
                "=> PIT Score: 16 of 100");

        var writer = new GitHubPullRequestWriter();
        var output = mock(Output.class);
        writer.handleAnnotations(score, output);

        assertThat(score.getCoveredFiles(Metric.LINE)
                .stream()
                .map(FileNode::getMissedLineRanges)
                .flatMap(Collection::stream).collect(Collectors.toList()))
                .hasToString("[[15-27], [62-79], [102-103], [23-49], [13-15], [19-68], [16-27], "
                        + "[41-140], [152-153], [160-160], [164-166], [17-32], [40-258]]")
                .hasSize(13);
        assertThat(score.getCoveredFiles(Metric.BRANCH)
                .stream()
                .map(FileNode::getPartiallyCoveredLines)
                .filter(Predicate.not(Map::isEmpty))
                .map(Map::keySet)
                .flatMap(Collection::stream)).containsExactlyInAnyOrder(146, 159);
        assertThat(score.getCoveredFiles(Metric.MUTATION)
                .stream()
                .map(FileNode::getSurvivedMutationsPerLine)
                .filter(Predicate.not(Map::isEmpty))
                .map(Map::keySet)
                .flatMap(Collection::stream)).containsExactlyInAnyOrder(147, 29);
        verify(output, times(13 + 2 + 2)).add(any(Annotation.class));
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
