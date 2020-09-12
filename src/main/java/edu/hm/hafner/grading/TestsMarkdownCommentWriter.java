package edu.hm.hafner.grading;

import java.util.List;

import edu.hm.hafner.analysis.Report;

/**
 * Renders the test results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class TestsMarkdownCommentWriter {
    private static final String TESTS_HEADER = "## :vertical_traffic_light: Unit Test Score: ";

    /**
     * Renders the test results in Markdown.
     *
     * @param score
     *         Aggregated score
     * @param testReports
     *         JUnit test reports
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score, final List<Report> testReports) {
        if (!score.getTestConfiguration().isEnabled()) {
            return TESTS_HEADER + "not configured :vertical_traffic_light:\n";
        }
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(TESTS_HEADER)
                .append(score.getTestAchieved())
                .append(" / ")
                .append(score.getTestConfiguration().getMaxScore())
                .append(" :vertical_traffic_light:\n");
        stringBuilder.append(formatColumns(new String[] {"Passed", "Skipped", "Failed", "Impact"}));
        stringBuilder.append(formatColumns(new String[] {":-:", ":-:", ":-:", ":-:"}));
        score.getTestScores().forEach(testScore -> stringBuilder.append(formatColumns(new String[] {
                String.valueOf(testScore.getPassedSize()),
                String.valueOf(testScore.getSkippedSize()),
                String.valueOf(testScore.getFailedSize()),
                String.valueOf(testScore.getTotalImpact())})));

        if (score.getTestScores().stream().map(TestScore::getFailedSize).count() > 0) {
            stringBuilder.append("### Failures\n");
            testReports.stream().flatMap(Report::stream).forEach(
                    issue -> stringBuilder.append("- ")
                            .append(issue.getFileName())
                            .append("(")
                            .append(issue.getLineStart())
                            .append("):")
                            .append("\n```\n")
                            .append(issue.getMessage())
                            .append("\n```\n"));
        }
        return stringBuilder.toString();
    }

    private String formatColumns(final Object[] columns) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|%4$-10s|\n";
        return String.format(format, columns);
    }
}
