package edu.hm.hafner.grading.github;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.grading.AutoGradingRunner;
import edu.hm.hafner.grading.GradingReport;
import edu.hm.hafner.grading.QualityGateResult;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHCheckRun.Conclusion;
import org.kohsuke.github.GHCheckRun.Status;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GHCheckRunBuilder.Output;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpException;

/**
 * GitHub action entrypoint for the autograding action.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class GitHubAutoGradingRunner extends AutoGradingRunner {
    private static final String COMMENT_MARKER = "<!-- -[quality-monitor-comment]- -->";
    private static final ParserRegistry PARSER_REGISTRY = new ParserRegistry();
    private static final String AUTOGRADING_ACTION = "GitHub Autograding Action";
    private static final String NO_TITLE = "none";
    private static final String DEFAULT_TITLE_METRIC = "line";

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
    GitHubAutoGradingRunner(final PrintStream printStream) {
        super(printStream);
    }

    @Override
    protected String getDisplayName() {
        return AUTOGRADING_ACTION;
    }

    @Override
    protected void publishGradingResult(final AggregatedScore score, final QualityGateResult qualityGateResult,
            final FilteredLog log) {
        var errors = createErrorMessageMarkdown(log);
        var conclusion = determineConclusion(errors, qualityGateResult, log);
        var qualityGateDetails = qualityGateResult.createMarkdownSummary();
        var showHeaders = StringUtils.isNotBlank(getEnv("SHOW_HEADERS", log));
        var results = new GradingReport();
        addComment(score,
                results.getTextSummary(score, getChecksName()),
                results.getMarkdownDetails(score, getChecksName()) + errors + qualityGateDetails,
                results.getSubScoreDetails(score).toString() + errors + qualityGateDetails,
                results.getMarkdownSummary(score, getChecksName(), showHeaders) + errors + qualityGateDetails,
                conclusion, log);

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

            var githubBuilder = new GitHubBuilder().withAppInstallationToken(oAuthToken);
            String apiUrl = getEnv("GITHUB_API_URL", log);
            if (!apiUrl.isBlank()) {
                githubBuilder.withEndpoint(apiUrl);
            }

            var github = githubBuilder.build();
            var check = github.getRepository(repository)
                    .createCheckRun(createMetricsBasedTitle(score, conclusion, log), getCustomSha(log))
                    .withStatus(Status.COMPLETED)
                    .withStartedAt(Date.from(Instant.now()))
                    .withConclusion(conclusion);

            var summaryWithFooter = markdownSummary + "\n\n<hr />\n\nCreated by " + getVersionLink(log);
            var output = new Output(textSummary, summaryWithFooter).withText(markdownDetails);

            attachAnnotations(score, output, log);
            check.add(output);

            var checksResult = createChecksRun(log, check);

            commentPullRequest(prSummary, checksResult, repository, github, log);
        }
        catch (IOException exception) {
            logException(log, exception, "Could create GitHub comments");
        }
    }

    private void attachAnnotations(final AggregatedScore score, final Output output, final FilteredLog log) {
        if (getEnv("SKIP_ANNOTATIONS", log).isEmpty()) {
            var annotationBuilder = new GitHubAnnotationsBuilder(output, computeAbsolutePathPrefixToRemove(log), log);
            annotationBuilder.createAnnotations(score);
        }
    }

    private void commentPullRequest(final String prSummary, final String checksResult, final String repository,
            final GitHub github, final FilteredLog log) throws IOException {
        var prNumber = getEnv("PR_NUMBER", log);
        if (prNumber.isBlank()) {
            return;
        }

        var strategy = getEnv("COMMENTS_STRATEGY", log);
        var previousComment = findPreviousComment(github, repository, prNumber);

        if ((Strings.CI.equals(strategy, "REMOVE") || StringUtils.isEmpty(strategy))
                && previousComment.isPresent()) {
            previousComment.get().delete();
            log.logInfo("Successfully deleted previous comment for PR#" + prNumber);
        }

        var comment = createComment(prSummary, checksResult, log);
        if (Strings.CI.equals(strategy, "UPDATE") && previousComment.isPresent()) {
            previousComment.get().update(comment);
            log.logInfo("Successfully replaced comment for PR#" + prNumber);
            return;
        }

        github.getRepository(repository)
                .getPullRequest(Integer.parseInt(prNumber))
                .comment(comment);
        log.logInfo("Successfully created new comment for PR#" + prNumber);
    }

    private String createComment(final String prSummary, final String checksResult, final FilteredLog log) {
        var footer = "Created by %s. %s".formatted(getVersionLink(log), checksResult);
        return COMMENT_MARKER + "\n\n" + prSummary + "\n\n<hr />\n\n" + footer + "\n";
    }

    private Optional<GHIssueComment> findPreviousComment(final GitHub github,
            final String repository, final String prNumber) throws IOException {
        var comments = github.getRepository(repository)
                .getPullRequest(Integer.parseInt(prNumber))
                .listComments();
        for (var comment : comments) {
            if (comment.getBody().contains(COMMENT_MARKER)) {
                return Optional.of(comment);
            }
        }
        return Optional.empty();
    }

    private String createChecksRun(final FilteredLog log, final GHCheckRunBuilder check) {
        try {
            GHCheckRun run = check.create();
            log.logInfo("Successfully created check " + run);

            return "More details are shown in the [GitHub Checks Result](%s).".formatted(
                    run.getDetailsUrl().toString());
        }
        catch (IOException exception) {
            logException(log, exception, "Could not create check");

            return "A detailed GitHub Checks Result could not be created, see error log.";
        }
    }

    private void logException(final FilteredLog log, final IOException exception, final String message) {
        String errorMessage;
        if (exception instanceof HttpException responseException) {
            errorMessage = StringUtils.defaultIfBlank(responseException.getResponseMessage(), exception.getMessage());
        }
        else {
            errorMessage = exception.getMessage();
        }
        log.logError("%s: %s", message, StringUtils.defaultIfBlank(errorMessage, "no error message available"));
    }

    private String getVersionLink(final FilteredLog log) {
        var version = readVersion(log);
        var sha = readSha(log);
        return "[%s](https://github.com/uhafner/autograding-github-action/releases/tag/v%s) v%s (#%s)"
                .formatted(getDisplayName(), version, version, sha);
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

    /**
     * Gets the SHA to use for the quality monitor check. First checks for a custom SHA (SHA) which takes precedence
     * over the default GITHUB_SHA. This allows workflows to override the SHA used for quality monitoring when needed.
     *
     * @param log
     *         the logger
     *
     * @return the SHA to use for the check
     */
    private String getCustomSha(final FilteredLog log) {
        String customSha = getEnv("SHA", log);
        if (!customSha.isBlank()) {
            log.logInfo("Using custom SHA from SHA: " + customSha);
            return customSha;
        }

        String defaultSha = getEnv("GITHUB_SHA", log);
        log.logInfo("Using default SHA from GITHUB_SHA: " + defaultSha);
        return defaultSha;
    }

    /**
     * Determines the GitHub check conclusion based on errors and quality gate results.
     *
     * @param errors
     *         the error messages
     * @param qualityGateResult
     *         the quality gate evaluation result
     * @param log
     *         the logger
     *
     * @return the conclusion
     */
    private Conclusion determineConclusion(final String errors, final QualityGateResult qualityGateResult,
            final FilteredLog log) {
        if (!errors.isBlank()) {
            log.logInfo("Setting conclusion to FAILURE due to errors in log");
            return Conclusion.FAILURE;
        }

        return switch (qualityGateResult.getOverallStatus()) {
            case FAILURE -> {
                log.logInfo("Setting conclusion to FAILURE due to quality gate failures");
                yield Conclusion.FAILURE;
            }
            case UNSTABLE -> {
                log.logInfo("Setting conclusion to NEUTRAL due to quality gate warnings");
                yield Conclusion.NEUTRAL;
            }
            default -> {
                log.logInfo("Setting conclusion to SUCCESS - all quality gates passed");
                yield Conclusion.SUCCESS;
            }
        };
    }

    /**
     * Creates a title based on the metrics.
     *
     * @param score
     *         the aggregated score
     * @param conclusion
     *         the conclusion
     * @param log
     *         the logger
     *
     * @return the title
     */
    private String createMetricsBasedTitle(final AggregatedScore score, final Conclusion conclusion,
            final FilteredLog log) {
        var titleMetric = StringUtils.defaultIfBlank(
                StringUtils.lowerCase(getEnv("TITLE_METRIC", log)),
                DEFAULT_TITLE_METRIC);

        if (NO_TITLE.equals(titleMetric)) {
            if (conclusion != Conclusion.SUCCESS) {
                return getChecksName() + " - Quality gates failed";
            }
            return getChecksName();
        }

        var metrics = score.getMetrics();

        if (!metrics.containsKey(titleMetric)) {
            log.logError("Requested title metric '%s' not found in metrics: %s", titleMetric, metrics.keySet());
            log.logError("Falling back to default metric %s", DEFAULT_TITLE_METRIC);

            titleMetric = DEFAULT_TITLE_METRIC; // Fallback to default metric
        }

        if (metrics.containsKey(titleMetric)) {
            var value = metrics.get(titleMetric);
            if (PARSER_REGISTRY.contains(titleMetric)) {
                return String.format(Locale.ENGLISH, "%s - %s: %d", getChecksName(),
                        PARSER_REGISTRY.get(titleMetric).getName(), value);
            }
            try {
                var metric = Metric.fromName(titleMetric);
                return String.format(Locale.ENGLISH, "%s - %s: %s", getChecksName(),
                        metric.getDisplayName(), metric.format(Locale.ENGLISH, value));
            }
            catch (IllegalArgumentException exception) {
                return String.format(Locale.ENGLISH, "%s - %s: %d", getChecksName(),
                        titleMetric, value);
            }
        }
        log.logInfo("Requested title metric '%s' not found in metrics: %s", titleMetric, metrics.keySet());

        return getChecksName();
    }
}
