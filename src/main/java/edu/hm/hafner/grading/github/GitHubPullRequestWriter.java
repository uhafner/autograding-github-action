package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.grading.AnalysisScore;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.AnnotationLevel;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCheckRunBuilder.Annotation;
import org.kohsuke.github.GHCheckRunBuilder.Output;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

/**
 * Writes a comment in a pull request.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class GitHubPullRequestWriter {
    /**
     * Writes the specified comment as GitHub checks result. Requires that the environment variables {@code HEAD_SHA},
     * {@code GITHUB_SHA}, {@code GITHUB_REPOSITORY}, and {@code TOKEN} are correctly set.
     *
     * @param name
     *         the name of the checks result
     * @param header
     *         the header of the check
     * @param summary
     *         the summary of the check
     * @param comment
     *         the details of the check (supports Markdown)
     */
    public void addComment(final String name, final AggregatedScore score, final String header, final String summary, final String comment) {
        String repository = System.getenv("GITHUB_REPOSITORY");
        System.out.println(">>>> GITHUB_REPOSITORY: " + repository);
        if (repository == null) {
            System.out.println("No GITHUB_REPOSITORY defined - skipping");

            return;
        }
        String oAuthToken = System.getenv("TOKEN");
        if (oAuthToken == null) {
            System.out.println("No valid TOKEN found - skipping");
        }

        String sha = System.getenv("GITHUB_SHA");
        System.out.println(">>>> GITHUB_SHA: " + sha);

        String prSha = System.getenv("HEAD_SHA");
        System.out.println(">>>> HEAD_SHA: " + prSha);

        String actualSha = StringUtils.defaultIfBlank(prSha, sha);
        System.out.println(">>>> ACTUAL_SHA: " + actualSha);

        String workspace = System.getenv("GITHUB_WORKSPACE");
        System.out.println(">>>> GITHUB_WORKSPACE: " + workspace);

        String filesPrefix = getEnv("FILES_PREFIX");
        System.out.println(">>>> FILES_PREFIX: " + filesPrefix);

        try {
            GitHub github = new GitHubBuilder().withAppInstallationToken(oAuthToken).build();
            GHCheckRunBuilder check = github.getRepository(repository)
                    .createCheckRun(name, actualSha)
                    .withStatus(Status.COMPLETED)
                    .withStartedAt(Date.from(Instant.now()))
                    .withConclusion(Conclusion.SUCCESS);

            Pattern prefix = Pattern.compile(
                    "^.*" + StringUtils.substringAfterLast(repository, '/') + "/" + filesPrefix);
            Output output = new Output(header, summary).withText(comment);

            handleAnnotations(score, prefix, output);

            check.add(output);
            GHCheckRun run = check.create();

            System.out.println("Successfully created check " + run);
        }
        catch (IOException exception) {
            System.out.println("Could not create check due to " + exception);
        }
    }

    private void handleAnnotations(final AggregatedScore score, final Pattern prefix, final Output output) {
        if (getEnv("SKIP_ANNOTATIONS").isEmpty()) {
            score.getAnalysisScores().stream()
                    .map(AnalysisScore::getReport)
                    .flatMap(Report::stream)
                    .map(issue -> createAnnotation(prefix, issue))
                    .forEach(output::add);
        }
    }

    private String getEnv(final String env) {
        return StringUtils.defaultString(System.getenv(env));
    }

    private Annotation createAnnotation(final Pattern prefix, final Issue issue) {
        Annotation annotation = new Annotation(prefix.matcher(issue.getFileName()).replaceAll(""),
                issue.getLineStart(), issue.getLineEnd(),
                AnnotationLevel.WARNING, issue.getMessage()).withTitle(issue.getType());
        if (issue.getLineStart() == issue.getLineEnd()) {
            return annotation.withStartColumn(issue.getColumnStart()).withEndColumn(issue.getColumnEnd());
        }
        return annotation;
    }
}
