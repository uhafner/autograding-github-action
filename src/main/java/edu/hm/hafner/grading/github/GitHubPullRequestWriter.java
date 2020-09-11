package edu.hm.hafner.grading.github;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.egit.github.core.service.IssueService;

import edu.hm.hafner.util.IntegerParser;

import de.tobiasmichael.me.ResultParser.ResultParser;

/**
 * Writes a comment in a pull request.
 *
 * @author Ullrich Hafner
 */
public class GitHubPullRequestWriter {
    public void addComment(final String comment) {
        String ref = System.getenv("GITHUB_REF");
        if (ref == null) {
            System.out.println("No GITHUB_REF defined - skipping");

            return;
        }
        System.out.println(">>>> GITHUB_REF: " + ref);
        String[] refElements = StringUtils.split(ref, "/");
        if (refElements.length < 3) {
            System.out.println("Cannot parse GITHUB_REF");

            return;
        }

        if (!"pull".equals(refElements[1])) {
            System.out.println("Action not executed within context of a pull request");

            return;
        }

        int pullRequest = IntegerParser.parseInt(refElements[2]);
        if (pullRequest < 1) {
            System.out.println(pullRequest + " is not a valid pull request number");

            return;
        }

        String repository = System.getenv("GITHUB_REPOSITORY");
        if (repository == null) {
            System.out.println("No GITHUB_REPOSITORY defined - skipping");

            return;
        }
        String[] repositoryElements = StringUtils.split(repository, "/");
        if (repositoryElements.length < 2) {
            System.out.println("Cannot parse GITHUB_REPOSITORY: " + repository);

            return;
        }

        String sha = System.getenv("GITHUB_SHA");
        System.out.println(">>>> GITHUB_SHA: " + sha);

        String oAuthToken = ResultParser.getOAuthToken();
        if (oAuthToken != null) {
            writeComment(comment, pullRequest, repositoryElements, oAuthToken);
        }
        else {
            System.out.println("No valid TOKEN found - skipping");
        }
    }

    private void writeComment(final String comment, final int pullRequest, final String[] repositoryElements,
            final String oAuthToken) {
        try {
            IssueService service = new IssueService();
            service.getClient().setOAuth2Token(oAuthToken);
            service.createComment(repositoryElements[0], repositoryElements[1], pullRequest, comment);
        }
        catch (IOException exception) {
            System.out.println("Can't write comment to GitHub:");
            exception.printStackTrace();
        }
    }
}
