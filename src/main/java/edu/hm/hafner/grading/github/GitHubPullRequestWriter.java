package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.egit.github.core.service.IssueService;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCheckRunBuilder.Output;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import edu.hm.hafner.util.IntegerParser;

/**
 * Writes a comment in a pull request.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class GitHubPullRequestWriter {
    /**
     * Writes the specified comment to the GitHub pull request. Requires that the environment variables {@code
     * GITHUB_REF}, {@code GITHUB_REPOSITORY}, and {@code TOKEN} are correctly set.
     *
     * @param comment
     *         the comment to write, supports GitHub Markdown
     */
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


        String oAuthToken = System.getenv("TOKEN");
        if (oAuthToken == null) {
            System.out.println("No valid TOKEN found - skipping");
        }

        String sha = System.getenv("GITHUB_SHA");
        System.out.println(">>>> GITHUB_SHA: " + sha);

        String prSha = System.getenv("PR_SHA");
        System.out.println(">>>> PR_SHA: " + prSha);

        writeComment(comment, pullRequest, repositoryElements, oAuthToken);

        writeChecks(comment, oAuthToken, repository, StringUtils.defaultString(prSha, sha));
    }

    private void writeChecks(final String comment, final String token, final String repositoryUrl,
            final String sha) {
        try {
            GitHub github = new GitHubBuilder().withAppInstallationToken(token).build();
            GHCheckRunBuilder check = github.getRepository(repositoryUrl)
                    .createCheckRun("Autograding", sha)
                    .withStatus(Status.COMPLETED)
                    .withStartedAt(Date.from(Instant.now()))
                    .withConclusion(Conclusion.SUCCESS);
            check.add(new Output("Autograding results",
                    "Summary")
                    .withText(comment));
            GHCheckRun run = check.create();

            System.out.println(run);
        }
        catch (IOException exception) {
            System.out.println(exception);
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
