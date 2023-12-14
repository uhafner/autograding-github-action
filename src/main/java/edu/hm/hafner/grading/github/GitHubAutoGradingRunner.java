package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.grading.AutoGradingRunner;
import edu.hm.hafner.grading.GradingReport;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCheckRunBuilder.Output;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

/**
 * GitHub action entrypoint for the autograding action.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class GitHubAutoGradingRunner extends AutoGradingRunner {
    /**
     * Public entry point for the GitHub action in the docker container, simply calls the action.
     *
     * @param unused
     *         not used
     */
    public static void main(final String... unused) {
        new GitHubAutoGradingRunner().run();
    }

    private AggregatedScore aggregation = new AggregatedScore("{}", new FilteredLog("unused"));  // temporary result just for testing

    /**
     * Creates a new instance of {@link GitHubAutoGradingRunner}.
     */
    public GitHubAutoGradingRunner() {
        super();
    }

    @VisibleForTesting
    GitHubAutoGradingRunner(final PrintStream printStream) {
        super(printStream);
    }

    @CheckForNull @VisibleForTesting
    AggregatedScore getAggregation() {
        return aggregation;
    }

    @Override
    protected void publishGradingResult(final AggregatedScore score, final FilteredLog log) {
        var results = new GradingReport();

        var errors = createErrorMessageMarkdown(log);

        addComment(score,
                results.getHeader(), results.getTextSummary(score),
                results.getMarkdownDetails(score) + errors,
                results.getMarkdownSummary(score, ":mortar_board: " + getChecksName()) + errors,
                errors.isBlank() ? Conclusion.SUCCESS : Conclusion.FAILURE, log);

        try {
            var environmentVariables = createEnvironmentVariables(score, log);
            Files.writeString(Paths.get("metrics.env"), environmentVariables);
        }
        catch (IOException exception) {
            log.logException(exception, "Can't write environment variables to 'metrics.env'");
        }

        log.logInfo("GitHub Action has finished");
        aggregation = score;
    }

    String createEnvironmentVariables(final AggregatedScore score, final FilteredLog log) {
        var metrics = new StringBuilder();
        score.getMetrics().forEach((metric, value) -> metrics.append(String.format("%s=%d%n", metric, value)));
        log.logInfo("------------------------------------------------------------------");
        log.logInfo("--------------------------- Summary ------------------------------");
        log.logInfo("------------------------------------------------------------------");
        log.logInfo(metrics.toString());
        return metrics.toString();
    }

    @Override
    protected void publishError(final AggregatedScore score, final FilteredLog log, final Throwable exception) {
        var results = new GradingReport();

        addComment(score,
                results.getHeader(), results.getTextSummary(score),
                results.getMarkdownErrors(score, exception),
                results.getMarkdownErrors(score, exception),
                Conclusion.FAILURE, log);

        aggregation = score;
    }

    private String getChecksName() {
        return StringUtils.defaultIfBlank(System.getenv("CHECKS_NAME"), "Autograding results");
    }

    private void addComment(final AggregatedScore score,
            final String header, final String summary, final String comment, final String prComment,
            final Conclusion conclusion, final FilteredLog log) {
        var repository = getEnv("GITHUB_REPOSITORY", log);
        if (repository.isBlank()) {
            log.logError("No GITHUB_REPOSITORY defined - skipping");

            return;
        }
        String oAuthToken = getEnv("GITHUB_TOKEN", log);
        if (oAuthToken.isBlank()) {
            log.logError("No valid GITHUB_TOKEN found - skipping");

            return;
        }

        try {
            String sha = getEnv("GITHUB_SHA", log);
            GitHub github = new GitHubBuilder().withAppInstallationToken(oAuthToken).build();
            GHCheckRunBuilder check = github.getRepository(repository)
                    .createCheckRun(getChecksName(), sha)
                    .withStatus(Status.COMPLETED)
                    .withStartedAt(Date.from(Instant.now()))
                    .withConclusion(conclusion);

            Output output = new Output(header, summary).withText(comment);

            if (getEnv("SKIP_ANNOTATIONS", log).isEmpty()) {
                var annotationBuilder = new GitHubAnnotationsBuilder(output, computeAbsolutePathPrefixToRemove(log));
                annotationBuilder.createAnnotations(score);
            }
            check.add(output);
            GHCheckRun run = check.create();

            log.logInfo("Successfully created check " + run);

            var prNumber = getEnv("PR_NUMBER", log);
            if (!prNumber.isBlank()) { // optional PR comment
                github.getRepository(repository)
                        .getPullRequest(Integer.parseInt(prNumber))
                        .comment(prComment + addCheckLink(run));
                log.logInfo("Successfully commented PR#" + prNumber);
            }
        }
        catch (IOException exception) {
            log.logException(exception, "Could not create check");
        }
    }

    private String computeAbsolutePathPrefixToRemove(final FilteredLog log) {
        return String.format("%s/%s/", getEnv("RUNNER_WORKSPACE", log),
                StringUtils.substringAfter(getEnv("GITHUB_REPOSITORY", log), "/"));
    }

    private String addCheckLink(final GHCheckRun run) {
        return String.format("## %n%n More details are available in the [GitHub Checks Result](%s).%n",
                run.getDetailsUrl().toString());
    }

    private String getEnv(final String key, final FilteredLog log) {
        String value = StringUtils.defaultString(System.getenv(key));
        log.logInfo(">>>> " + key + ": " + value);
        return value;
    }
}
