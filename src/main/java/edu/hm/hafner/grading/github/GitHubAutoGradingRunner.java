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

    /**
     * Creates a new instance of {@link GitHubAutoGradingRunner}.
     */
    public GitHubAutoGradingRunner() {
        super();
    }

    @VisibleForTesting
    protected GitHubAutoGradingRunner(final PrintStream printStream) {
        super(printStream);
    }

    @Override
    protected void publishGradingResult(final AggregatedScore score, final FilteredLog log) {
        var errors = createErrorMessageMarkdown(log);

        var results = new GradingReport();
        addComment(score,
                results.getTextSummary(score, getChecksName()),
                results.getMarkdownDetails(score, getChecksName()) + errors,
                results.getSubScoreDetails(score) + errors,
                results.getMarkdownSummary(score, getChecksName()) + errors,
                errors.isBlank() ? Conclusion.SUCCESS : Conclusion.FAILURE, log);

        try {
            var environmentVariables = createEnvironmentVariables(score, log);
            Files.writeString(Paths.get("metrics.env"), environmentVariables);
        }
        catch (IOException exception) {
            log.logException(exception, "Can't write environment variables to 'metrics.env'");
        }

        log.logInfo("GitHub Action has finished");
    }

    @Override
    protected void publishError(final AggregatedScore score, final FilteredLog log, final Throwable exception) {
        var results = new GradingReport();

        var markdownErrors = results.getMarkdownErrors(score, exception);
        addComment(score, results.getTextSummary(score, getChecksName()),
                markdownErrors, markdownErrors, markdownErrors, Conclusion.FAILURE, log);
    }

    private void addComment(final AggregatedScore score, final String textSummary,
            final String markdownDetails, final String markdownSummary, final String prSummary,
            final Conclusion conclusion, final FilteredLog log) {
        try {
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

            String sha = getEnv("GITHUB_SHA", log);

            GitHub github = new GitHubBuilder().withAppInstallationToken(oAuthToken).build();
            GHCheckRunBuilder check = github.getRepository(repository)
                    .createCheckRun(getChecksName(), sha)
                    .withStatus(Status.COMPLETED)
                    .withStartedAt(Date.from(Instant.now()))
                    .withConclusion(conclusion);

            var summaryWithFooter = markdownSummary + "\n\nCreated by " + getVersionLink(log);
            Output output = new Output(textSummary, summaryWithFooter).withText(markdownDetails);

            if (getEnv("SKIP_ANNOTATIONS", log).isEmpty()) {
                var annotationBuilder = new GitHubAnnotationsBuilder(
                        output, computeAbsolutePathPrefixToRemove(log), log);
                annotationBuilder.createAnnotations(score);
            }

            check.add(output);

            GHCheckRun run = check.create();
            log.logInfo("Successfully created check " + run);

            var prNumber = getEnv("PR_NUMBER", log);
            if (!prNumber.isBlank()) { // optional PR comment
                var footer = "Created by %s. More details are shown in the [GitHub Checks Result](%s)."
                        .formatted(getVersionLink(log), run.getDetailsUrl().toString());
                github.getRepository(repository)
                        .getPullRequest(Integer.parseInt(prNumber))
                        .comment(prSummary + "\n\n" + footer + "\n");
                log.logInfo("Successfully commented PR#" + prNumber);
            }
        }
        catch (IOException exception) {
            log.logException(exception, "Could not create check");
        }
    }

    private String getVersionLink(final FilteredLog log) {
        var version = readVersion(log);
        var sha = readSha(log);
        return "[%s](https://github.com/uhafner/autograding-github-action/releases/tag/v%s) v%s (#%s)"
                .formatted(getDisplayName(), version, version, sha);
    }

    String createEnvironmentVariables(final AggregatedScore score, final FilteredLog log) {
        var metrics = new StringBuilder();
        score.getMetrics().forEach((metric, value) -> metrics.append(String.format("%s=%d%n", metric, value)));
        log.logInfo("---------------");
        log.logInfo("Metrics Summary");
        log.logInfo("---------------");
        log.logInfo(metrics.toString());
        return metrics.toString();
    }

    private String getChecksName() {
        return StringUtils.defaultIfBlank(System.getenv("CHECKS_NAME"), getDisplayName());
    }

    private String computeAbsolutePathPrefixToRemove(final FilteredLog log) {
        return String.format("%s/%s/", getEnv("RUNNER_WORKSPACE", log),
                StringUtils.substringAfter(getEnv("GITHUB_REPOSITORY", log), "/"));
    }

    private String getEnv(final String key, final FilteredLog log) {
        String value = StringUtils.defaultString(System.getenv(key));
        log.logInfo(">>>> " + key + ": " + value);
        return value;
    }
}
