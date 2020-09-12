package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;

/**
 * Creates a summary comment in Markdown.
 *
 * @author Tobias Effner
 */
public class Summary {
    /**
     * Creates a summary comment in Markdown.
     *
     * @param score
     *         the aggregated score
     * @param testReports
     *         JUnit reports that many contain details about failed tests
     *
     * @return comment formatted with Markdown
     */
    public String create(final AggregatedScore score, final List<Report> testReports) {
        StringBuilder comment = new StringBuilder();

        comment.append("# ").append(score.toString()).append("\n");

        TestsMarkdownCommentWriter testWriter = new TestsMarkdownCommentWriter();
        comment.append(testWriter.create(score, testReports));

        if (score.getPitConfiguration().isEnabled()
                && testReports.stream().anyMatch(report -> report.getCounter(JUnitAdapter.FAILED_TESTS) > 0)) {
            comment.append("## PIT Mutation: Not available!\n");
            comment.append(":warning: This means you did not pass all Unit tests! :warning:\n");
        }
        else {
            PitMarkdownCommentWriter pitWriter = new PitMarkdownCommentWriter();
            comment.append(pitWriter.create(score));
        }

        CoverageMarkdownCommentWriter coverageWriter = new CoverageMarkdownCommentWriter();
        comment.append(coverageWriter.create(score));

        AnalysisMarkdownCommentWriter analysisMarkdown = new AnalysisMarkdownCommentWriter();
        comment.append(analysisMarkdown.create(score));

        return comment.toString();
    }
}
