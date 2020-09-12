package edu.hm.hafner.grading;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Report;

/**
 * Renders the test results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class TestsMarkdownCommentWriter {
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
        if (score.getTestConfiguration().isEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("## :traffic_light: Unit Test Score: ")
                    .append(score.getTestAchieved())
                    .append(" / ")
                    .append(score.getTestConfiguration().getMaxScore())
                    .append("\n");
            stringBuilder.append(formatColumns(new String[] {"Passed", "Skipped", "Failed", "Impact"}));
            stringBuilder.append(formatColumns(new String[] {":-:", ":-:", ":-:", ":-:"}));
            score.getTestScores().forEach(testScore -> stringBuilder.append(formatColumns(new String[] {
                    String.valueOf(testScore.getPassedSize()),
                    String.valueOf(testScore.getSkippedSize()),
                    String.valueOf(testScore.getFailedSize()),
                    String.valueOf(testScore.getTotalImpact())})));
            stringBuilder.append("\n___\n");
            testReports.stream().flatMap(Report::stream).forEach(
                    issue -> stringBuilder.append("- ")
                            .append(issue.getFileName())
                            .append("(")
                            .append(issue.getLineStart())
                            .append("):")
                            .append("\n```\n")
                            .append(issue.getMessage())
                            .append("\n```\n"));
            stringBuilder.append("\n___\n");
            return stringBuilder.toString();
        }
        return StringUtils.EMPTY;
    }

    private String formatColumns(final Object[] columns) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|%4$-10s|\n";
        return String.format(format, columns);
    }
}
