package de.tobiasmichael.me.GithubComment;


import de.tobiasmichael.me.ResultParser.ResultParser;
import org.eclipse.egit.github.core.client.GitHubClient;

/**
 * This class will work against the GitHub API and comment the pull request
 */
public class Commenter {

    private final String comment;

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


    public void commentTo() {
        String oAuthToken = ResultParser.getoAuthToken();
        if (oAuthToken != null) {
            GitHubClient gitHubClient = new GitHubClient();
            gitHubClient.setOAuth2Token(oAuthToken);
        }


        System.out.print(comment);
    }

}
