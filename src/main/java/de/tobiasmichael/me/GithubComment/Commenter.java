package de.tobiasmichael.me.GithubComment;


import de.tobiasmichael.me.ResultParser.ResultParser;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.grading.AggregatedScore;
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

    public Commenter(String comment) {
        this();
        this.comment = formatComment(comment);
    }

    public Commenter(String comment, Throwable err) {
        this();
        this.comment = formatComment(comment);
        err.printStackTrace();
    }

    public Commenter(String comment, List<Report> reportList) {
        this();
        this.comment = formatComment(comment, reportList);
    }

    public Commenter(AggregatedScore score) {
        this();
        this.comment = formatComment(score);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Formats the given comment to a readable string.
     *
     * @param comment comment string
     * @return returns readable string
     */
    private String formatComment(String comment) {
        return "___________________\n" + comment + "\n___________________\n";
    }

    /**
     * Formats the given comment and ReportList to a markdown string.
     *
     * @param comment comment string
     * @param reportList list of reports
     * @return returns readable string
     */
    private String formatComment(String comment, List<Report> reportList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(comment);
        stringBuilder.append("\n___________________\n");
        for (Report report : reportList) {
            report.forEach(issue -> {
                stringBuilder.append("- ");
                stringBuilder.append(issue);
                stringBuilder.append("\n");
            });
        }
        stringBuilder.append("___________________\n");
        return stringBuilder.toString();
    }

    /**
     * Formats the given score to a markdown string.
     *
     * @param score AggregatedScore
     * @return returns readable string
     */
    private String formatComment(AggregatedScore score) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("# ").append(score.toString()).append("\n");

        stringBuilder.append("### Unit Tests: ").append(score.getTestRatio()).append("\n");
        stringBuilder.append(tableFormat(new String[]{"Failed", "Passed", "Impact"}));
        stringBuilder.append(tableFormat(new String[]{":-:", ":-:", ":-:"}));
        score.getTestScores().forEach(testScore -> {
            stringBuilder.append(tableFormat(new String[]{String.valueOf(testScore.getFailedSize()),
                    String.valueOf(testScore.getPassedSize()),
                    String.valueOf(testScore.getTotalImpact())}));
        });
        stringBuilder.append("\n___\n");

        stringBuilder.append("### Coverage Score: ").append(score.getCoverageRatio()).append("\n");
        stringBuilder.append(tableFormat(new String[]{"Name", "Covered", "Impact"}));
        stringBuilder.append(tableFormat(new String[]{":-:", ":-:", ":-:"}));
        score.getCoverageScores().forEach(coverageScore -> {
            stringBuilder.append(tableFormat(new String[]{coverageScore.getName(),
                    String.valueOf(coverageScore.getCoveredPercentage()),
                    String.valueOf(coverageScore.getTotalImpact())}));
        });
        stringBuilder.append("\n___\n");

        stringBuilder.append("### PIT Mutation: ").append(score.getPitRatio()).append("\n");
        stringBuilder.append(tableFormat(new String[]{"Detected", "Undetected", "Impact"}));
        stringBuilder.append(tableFormat(new String[]{":-:", ":-:", ":-:"}));
        score.getPitScores().forEach(pitScore -> {
            stringBuilder.append(tableFormat(new String[]{String.valueOf(pitScore.getDetectedPercentage()),
                    String.valueOf(pitScore.getUndetectedPercentage()),
                    String.valueOf(pitScore.getTotalImpact())}));
        });
        stringBuilder.append("\n___\n");

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
        try {
            String pull_request_number = System.getenv("GITHUB_REF").split("/")[2];
            String repo_owner_and_name = System.getenv("GITHUB_REPOSITORY");

            String oAuthToken = ResultParser.getOAuthToken();
            IssueService service = new IssueService();
            if (oAuthToken != null) {
                service.getClient().setOAuth2Token(oAuthToken);
            }
            RepositoryId repo = new RepositoryId(repo_owner_and_name.split("/")[0], repo_owner_and_name.split("/")[1]);
            service.createComment(repo.getOwner(), repo.getName(), Integer.parseInt(pull_request_number), comment);
        } catch (NullPointerException ignore) {
            logger.warning("Not in Github actions, so not going to execute comment.");
            logger.info(comment);
        } catch (IOException e) {
            logger.severe(e.toString());
        }
    }

}
