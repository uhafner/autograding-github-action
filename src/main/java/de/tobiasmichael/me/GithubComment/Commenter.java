package de.tobiasmichael.me.GithubComment;


import de.tobiasmichael.me.ResultParser.ResultParser;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;

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

    public void setComment(String comment) {
        this.comment = comment;
    }

    private String formatComment(String comment) {
        return "___________________\n" + comment + "\n___________________\n";
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
