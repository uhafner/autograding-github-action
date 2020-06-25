package de.tobiasmichael.me.GithubComment;


import de.tobiasmichael.me.ResultParser.ResultParser;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

/**
 * This class will work against the GitHub API and comment the pull request
 */
public class Commenter {

    private final String comment;

    public Commenter(String comment) {
        this.comment = formatComment(comment);
    }

    public Commenter(String comment, Throwable err) {
        this.comment = formatComment(comment);
        System.out.println(err.toString());
    }


    private String formatComment(String comment) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("___________________\n");
        stringBuilder.append(comment);
        stringBuilder.append("\n___________________\n");

        return stringBuilder.toString();
    }


    public void commentTo() throws IOException {
        String pull_request_number = System.getenv("GITHUB_REF").split("/")[2];
        String repo_owner_and_name = System.getenv("GITHUB_REPOSITORY");

        String oAuthToken = ResultParser.getoAuthToken();
        if (oAuthToken != null) {
            IssueService service = new IssueService();
            service.getClient().setOAuth2Token(oAuthToken);
            RepositoryId repo = new RepositoryId(repo_owner_and_name.split("/")[0], repo_owner_and_name.split("/")[1]);
            service.createComment(repo.getOwner(), repo.getName(), Integer.parseInt(pull_request_number), comment);
        }
    }

}
