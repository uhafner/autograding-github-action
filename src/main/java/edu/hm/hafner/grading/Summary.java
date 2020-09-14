package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.analysis.Report;

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
     * @param analysisReports
     *         static analysis reports with warnings
     *
     * @return comment formatted with Markdown
     */
    public String create(final AggregatedScore score, final List<Report> testReports, final List<Report> analysisReports) {
        StringBuilder comment = new StringBuilder();
        comment.append("# ").append(score.toString()).append("\n");

        TestsMarkdownCommentWriter testWriter = new TestsMarkdownCommentWriter();
        comment.append(testWriter.create(score, testReports));

        CoverageMarkdownCommentWriter coverageWriter = new CoverageMarkdownCommentWriter();
        comment.append(coverageWriter.create(score));

        PitMarkdownCommentWriter pitWriter = new PitMarkdownCommentWriter();
        comment.append(pitWriter.create(score));

        AnalysisMarkdownCommentWriter analysisMarkdown = new AnalysisMarkdownCommentWriter();
        comment.append(analysisMarkdown.create(score, analysisReports));

        return comment.toString();
    }
}
