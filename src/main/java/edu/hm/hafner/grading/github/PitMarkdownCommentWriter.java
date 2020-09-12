package edu.hm.hafner.grading.github;

import edu.hm.hafner.grading.AggregatedScore;

/**
 * Renders the PIT results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class PitMarkdownCommentWriter {
    /**
     * Renders the PIT mutation coverage results in Markdown.
     *
     * @param score
     *         the aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("## :microbe: PIT Mutation: ")
                .append(score.getPitAchieved())
                .append(" / ")
                .append(score.getPitConfiguration().getMaxScore())
                .append("\n");
        stringBuilder.append(formatColumns(new String[]{"Detected", "Undetected", "Detected %", "Undetected %", "Impact"}));
        stringBuilder.append(formatColumns(new String[]{":-:", ":-:", ":-:", ":-:", ":-:"}));
        score.getPitScores().forEach(pitScore -> {
            stringBuilder.append(formatColumns(new String[]{
                    String.valueOf(pitScore.getDetectedSize()),
                    String.valueOf(pitScore.getUndetectedSize()),
                    String.valueOf(pitScore.getDetectedPercentage()),
                    String.valueOf(pitScore.getUndetectedPercentage()),
                    String.valueOf(pitScore.getTotalImpact())}));
        });
        stringBuilder.append("\n___\n");
        return stringBuilder.toString();
    }

    private String formatColumns(final Object[] columns) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|%4$-10s|%5$-10s|\n";
        return String.format(format, columns);
    }
}
