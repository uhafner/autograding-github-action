package de.tobiasmichael.me.GithubComment;


import de.tobiasmichael.me.ResultParser.ResultParser;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;
import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;
import edu.hm.hafner.grading.github.TestsMarkdownCommentWriter;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class will work against the GitHub API and comment the pull request.
 *
 * @author Tobias Effner
 */
public class Commenter {

    private final Logger logger;
    private String comment;

    public Commenter() {
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(Level.ALL);
    }

    public Commenter(AggregatedScore score, List<Report> reportList) {
        this();
        this.comment = formatComment(score, reportList);
    }


    /**
     * Formats the given score to a markdown string.
     *
     * @param score AggregatedScore
     * @param testReports reportList of failed JUnit test
     * @return returns readable string
     */
    private String formatComment(AggregatedScore score, List<Report> testReports) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("# ").append(score.toString()).append("\n");
        TestsMarkdownCommentWriter testWriter = new TestsMarkdownCommentWriter();
        stringBuilder.append(testWriter.create(score, testReports));
        if (score.getPitConfiguration().isEnabled() 
                && testReports.stream().anyMatch(report -> report.getCounter(JUnitAdapter.FAILED_TESTS) > 0)) {
            stringBuilder.append("### PIT Mutation: Not available!\n");
            stringBuilder.append(":warning: This means you did not pass all Unit tests! :warning:\n");
        }
        stringBuilder.append(createCoverageComment(score));
        stringBuilder.append(createAnalysisComment(score));
        return stringBuilder.toString();
    }

    /**
     * Generates formatted string for Coverage.
     *
     * @param score Aggregated score
     * @return returns formatted string
     */
    private String createCoverageComment(AggregatedScore score) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("### Coverage Score: ").append(score.getCoverageRatio()).append("\n");
        stringBuilder.append(tableFormat(new String[]{"Name", "Covered", "Impact"}));
        stringBuilder.append(tableFormat(new String[]{":-:", ":-:", ":-:"}));
        score.getCoverageScores().forEach(coverageScore -> {
            stringBuilder.append(tableFormat(new String[]{coverageScore.getName(),
                    String.valueOf(coverageScore.getCoveredPercentage()),
                    String.valueOf(coverageScore.getTotalImpact())}));
        });
        stringBuilder.append("\n___\n");
        return stringBuilder.toString();
    }

    /**
     * Generates formatted string for Analysis.
     *
     * @param score Aggregated score
     * @return returns formatted string
     */
    private String createAnalysisComment(AggregatedScore score) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("### Static Analysis Warnings: ").append(score.getPitRatio()).append("\n");
        stringBuilder.append(tableFormat(new String[]{"Name", "Errors", "Impact"}));
        stringBuilder.append(tableFormat(new String[]{":-:", ":-:", ":-:"}));
        score.getAnalysisScores().forEach(analysisScore -> {
            stringBuilder.append(tableFormat(new String[]{analysisScore.getName(),
                    String.valueOf(analysisScore.getErrorsSize()),
                    String.valueOf(analysisScore.getTotalImpact())}));
        });
        return stringBuilder.toString();
    }

    /**
     * Generates formatted string for Mutations.
     *
     * @param score Aggregated score
     * @return returns formatted string
     */
    private String createMutationsComment(AggregatedScore score) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("### PIT Mutation: ").append(score.getPitRatio()).append("\n");
        stringBuilder.append(tableFormat(new String[]{"Detected", "Undetected", "Impact"}));
        stringBuilder.append(tableFormat(new String[]{":-:", ":-:", ":-:"}));
        score.getPitScores().forEach(pitScore -> {
            stringBuilder.append(tableFormat(new String[]{String.valueOf(pitScore.getDetectedPercentage()),
                    String.valueOf(pitScore.getUndetectedPercentage()),
                    String.valueOf(pitScore.getTotalImpact())}));
        });
        stringBuilder.append("\n___\n");
        return stringBuilder.toString();
    }

    /**
     * Converts 3 Strings to a formatted table string.
     *
     * @param strings 3 strings to format
     * @return returns formatted string
     */
    private String tableFormat(String[] strings) {
        String format = "|%1$-10s|%2$-10s|%3$-10s|\n";
        return String.format(format, (Object[]) strings);
    }

    /**
     * Gets the system variables from github actions and creates a comment
     * on the pull request with the formatted comment.
     * If there is no system variable set, the comment will be logged.
     * If there is no oAuthToken, the creation of the comment will be skipped.
     */
    public void commentTo() {
        GitHubPullRequestWriter writer = new GitHubPullRequestWriter();
        writer.addComment(comment);
    }
}
