package edu.hm.hafner.grading.github;

import edu.hm.hafner.grading.AggregatedScore;

/**
 * Renders the static analysis results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class AnalysisMarkdownCommentWriter {
    /**
     * Renders the static analysis results in Markdown.
     *
     * @param score
     *         the aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("## :warning: Static Analysis Warnings: ")
                .append(score.getAnalysisRatio())
                .append(" / ")
                .append(score.getAnalysisConfiguration().getMaxScore())
                .append("\n");
        stringBuilder.append(formatColumns(
                new String[] {"Name", "Errors", "Warning High", "Warning Normal", "Warning Low", "Impact"}));
        stringBuilder.append(formatColumns(new String[] {":-:", ":-:", ":-:", ":-:", ":-:", ":-:"}));
        score.getAnalysisScores().forEach(analysisScore -> stringBuilder.append(formatColumns(new String[] {
                analysisScore.getName(),
                String.valueOf(analysisScore.getErrorsSize()),
                String.valueOf(analysisScore.getHighSeveritySize()),
                String.valueOf(analysisScore.getNormalSeveritySize()),
                String.valueOf(analysisScore.getLowSeveritySize()),
                String.valueOf(analysisScore.getTotalImpact())})));
        return stringBuilder.toString();
    }

    private String formatColumns(final Object[] columns) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|%4$-10s|%5$-10s|%6$-10s|\n";
        return String.format(format, columns);
    }
}
