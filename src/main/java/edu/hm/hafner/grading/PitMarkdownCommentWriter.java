package edu.hm.hafner.grading;

/**
 * Renders the PIT results in Markdown.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class PitMarkdownCommentWriter {
    private static final String PIT_HEADER = "## :microbe: PIT Mutation Coverage: ";

    /**
     * Renders the PIT mutation coverage results in Markdown.
     *
     * @param score
     *         the aggregated score
     *
     * @return returns formatted string
     */
    public String create(final AggregatedScore score) {
        if (!score.getPitConfiguration().isEnabled()) {
            return PIT_HEADER + "not configured :microbe:\n";
        }

        StringBuilder comment = new StringBuilder();
        if (score.getTestScores().stream().map(TestScore::getFailedSize).count() > 0) {
            comment.append(PIT_HEADER)
                    .append(0)
                    .append(" / ")
                    .append(score.getPitConfiguration().getMaxScore())
                    .append(" :microbe:\n")
                    .append(":exclamation: PIT mutation coverage cannot be computed if there are test failures\n");
            return comment.toString();
        }

        comment.append(PIT_HEADER)
                .append(score.getPitAchieved())
                .append(" / ")
                .append(score.getPitConfiguration().getMaxScore())
                .append(" :microbe:\n");
        comment.append(formatColumns(new String[] {"Detected", "Undetected", "Detected %", "Undetected %", "Impact"}));
        comment.append(formatColumns(new String[] {":-:", ":-:", ":-:", ":-:", ":-:"}));
        score.getPitScores().forEach(pitScore -> comment.append(formatColumns(new String[] {
                String.valueOf(pitScore.getDetectedSize()),
                String.valueOf(pitScore.getUndetectedSize()),
                String.valueOf(pitScore.getDetectedPercentage()),
                String.valueOf(pitScore.getUndetectedPercentage()),
                String.valueOf(pitScore.getTotalImpact())})));
        return comment.toString();
    }

    private String formatColumns(final Object[] columns) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|%4$-10s|%5$-10s|\n";
        return String.format(format, columns);
    }
}
