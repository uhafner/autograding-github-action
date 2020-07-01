package de.tobiasmichael.me.GithubComment;


import de.tobiasmichael.me.ResultParser.ResultParser;
import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.List;

/**
 * This class will work against the GitHub API and comment the pull request
 */
public class Commenter {

    private String comment;

    public Commenter() {
    }

    public Commenter(String comment) {
        this.comment = formatComment(comment);
    }

    public Commenter(String comment, Throwable err) {
        this.comment = formatComment(comment);
        err.printStackTrace();
    }

    public Commenter(String comment, List<Report> reportList) {
        this.comment = formatComment(comment, reportList);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private String formatComment(String comment) {
        return "___________________\n" + comment + "\n___________________\n";
    }

    private String formatComment(String comment, List<Report> reportList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(comment);
        stringBuilder.append("___________________\n");
        for (Report report : reportList) {
            stringBuilder.append("- ");
            report.forEach(stringBuilder::append);
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public void commentTo() {
        try {
            String pull_request_number = System.getenv("GITHUB_REF").split("/")[2];
            String repo_owner_and_name = System.getenv("GITHUB_REPOSITORY");

            String oAuthToken = ResultParser.getOAuthToken();
            if (oAuthToken != null) {
                IssueService service = new IssueService();
                service.getClient().setOAuth2Token(oAuthToken);
                RepositoryId repo = new RepositoryId(repo_owner_and_name.split("/")[0], repo_owner_and_name.split("/")[1]);
                service.createComment(repo.getOwner(), repo.getName(), Integer.parseInt(pull_request_number), comment);
            }
        } catch (NullPointerException e) {
            System.out.println("Not in Github actions, so not going to execute comment.");
            System.out.println(comment);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
