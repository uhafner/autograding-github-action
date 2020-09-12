package de.tobiasmichael.me.GithubComment;


import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;
import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.grading.github.AnalysisMarkdownCommentWriter;
import edu.hm.hafner.grading.github.CoverageMarkdownCommentWriter;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;
import edu.hm.hafner.grading.github.PitMarkdownCommentWriter;
import edu.hm.hafner.grading.github.TestsMarkdownCommentWriter;

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
            stringBuilder.append("## PIT Mutation: Not available!\n");
            stringBuilder.append(":warning: This means you did not pass all Unit tests! :warning:\n");
        }
        else {
            PitMarkdownCommentWriter pitWriter = new PitMarkdownCommentWriter();
            stringBuilder.append(pitWriter.create(score));
        }

        CoverageMarkdownCommentWriter coverageWriter = new CoverageMarkdownCommentWriter();
        stringBuilder.append(coverageWriter.create(score));

        AnalysisMarkdownCommentWriter analysisMarkdown = new AnalysisMarkdownCommentWriter();
        stringBuilder.append(analysisMarkdown.create(score));

        return stringBuilder.toString();
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
