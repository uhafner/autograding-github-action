package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.analysis.Report;

/**
 * Creates a summary comment in Markdown.
 *
 * @author Tobias Effner
 */
public class GradingResults {
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
    public String createDetails(final AggregatedScore score, final List<Report> testReports) {
        StringBuilder comment = new StringBuilder();

        comment.append("# ").append(score.toString()).append("\n");

        TestsMarkdownCommentWriter testWriter = new TestsMarkdownCommentWriter();
        comment.append(testWriter.create(score, testReports));

        CoverageMarkdownCommentWriter coverageWriter = new CoverageMarkdownCommentWriter();
        comment.append(coverageWriter.create(score));

        PitMarkdownCommentWriter pitWriter = new PitMarkdownCommentWriter();
        comment.append(pitWriter.create(score));

        AnalysisMarkdownCommentWriter analysisMarkdown = new AnalysisMarkdownCommentWriter();
        comment.append(analysisMarkdown.create(score));

        return comment.toString();
    }

    /**
     * Returns the header for the GitHub check.
     *
     * @return the header
     */
    public String getHeader() {
        return "Autograding results";
    }

    /**
     * Returns the summary for the GitHub check.
     *
     * @param score
     *         the aggreagated score
     *
     * @return the summary
     */
    public String createSummary(final AggregatedScore score) {
        return String.format(
                "Total score: %d/%d (unit tests: %d/%d, coverage: %d/%d, mutation coverage: %d/%d, analysis: %d/%d)",
                score.getAchieved(), score.getTotal(),
                score.getTestAchieved(), score.getTestConfiguration().getMaxScore(),
                score.getCoverageAchieved(), score.getTestConfiguration().getMaxScore(),
                score.getPitAchieved(), score.getPitConfiguration().getMaxScore(),
                score.getAnalysisAchieved(), score.getAnalysisConfiguration().getMaxScore());

    }
}
