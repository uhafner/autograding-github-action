package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.grading.AutoGradingRunner;
import edu.hm.hafner.grading.Configuration;
import edu.hm.hafner.grading.GradingReport;
import edu.hm.hafner.grading.Score;
import edu.hm.hafner.grading.ToolConfiguration;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

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
 * GitHub action entrypoint for the autograding action.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
public class GitHubAutoGradingRunner extends AutoGradingRunner {
    private static final String GITHUB_WORKSPACE_REL = "/github/workspace/./";
    private static final String GITHUB_WORKSPACE_ABS = "/github/workspace/";

    /**
     * Public entry point for the GitHub action in the docker container, simply calls the action.
     *
     * @param unused
     *         not used
     */
    public static void main(final String... unused) {
        new GitHubAutoGradingRunner().run();
    }

    private transient AggregatedScore aggregation;  // temporary result just for testing

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
                Conclusion.SUCCESS, log);

        try {
            var environmentVariables = createEnvironmentVariables(score, log);
            Files.writeString(Paths.get("metrics.env"), environmentVariables, StandardOpenOption.CREATE);
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
            createAnnotations(score, log).forEach(output::add);

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

    private String addCheckLink(final GHCheckRun run) {
        return String.format("## %n%n More details are available in the [GitHub Checks Result](%s).%n",
                run.getDetailsUrl().toString());
    }

    private String getEnv(final String key, final FilteredLog log) {
        String value = StringUtils.defaultString(System.getenv(key));
        log.logInfo(">>>> " + key + ": " + value);
        return value;
    }

    @VisibleForTesting
    List<Annotation> createAnnotations(final AggregatedScore score, final FilteredLog log) {
        var annotations = new ArrayList<Annotation>();
        if (getEnv("SKIP_ANNOTATIONS", log).isEmpty()) {
            var prefix = computeAbsolutePathPrefixToRemove(log);

            var additionalAnalysisSourcePaths = extractAdditionalSourcePaths(score.getAnalysisScores());
            annotations.addAll(createAnnotationsForIssues(score, prefix, additionalAnalysisSourcePaths));

            var additionalCoverageSourcePaths = extractAdditionalSourcePaths(score.getCodeCoverageScores());
            annotations.addAll(createAnnotationsForMissedLines(score, prefix, additionalCoverageSourcePaths));
            annotations.addAll(createAnnotationsForPartiallyCoveredLines(score, prefix, additionalCoverageSourcePaths));

            var additionalMutationSourcePaths = extractAdditionalSourcePaths(score.getMutationCoverageScores());
            annotations.addAll(createAnnotationsForSurvivedMutations(score, prefix, additionalMutationSourcePaths));
        }
        return annotations;
    }

    private String computeAbsolutePathPrefixToRemove(final FilteredLog log) {
        return String.format("%s/%s/", getEnv("RUNNER_WORKSPACE", log),
                StringUtils.substringAfter(getEnv("GITHUB_REPOSITORY", log), "/"));
    }

    private Set<String> extractAdditionalSourcePaths(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getConfiguration)
                .map(Configuration::getTools)
                .flatMap(Collection::stream)
                .map(ToolConfiguration::getSourcePath).collect(Collectors.toSet());
    }

    private List<Annotation> createAnnotationsForIssues(final AggregatedScore score,
            final String prefix, final Set<String> sourcePaths) {
        return score.getIssues().stream()
                .map(issue -> createAnnotationForIssue(issue, prefix, sourcePaths))
                .toList();
    }

    private Annotation createAnnotationForIssue(final Issue issue,
            final String prefix, final Set<String> sourcePaths) {
        var path = createRelativeRepositoryPath(issue.getFileName(), prefix, sourcePaths);
        var relativePath = StringUtils.removeStart(
                StringUtils.removeStart(path, GITHUB_WORKSPACE_REL),
                GITHUB_WORKSPACE_ABS);

        Annotation annotation = new Annotation(relativePath,
                issue.getLineStart(), issue.getLineEnd(),
                AnnotationLevel.WARNING, issue.getMessage())
                .withTitle(issue.getOriginName() + ": " + issue.getType());

        if (issue.getLineStart() == issue.getLineEnd()) {
            return annotation.withStartColumn(issue.getColumnStart()).withEndColumn(issue.getColumnEnd());
        }

        return annotation;
    }

    private List<Annotation> createAnnotationsForMissedLines(final AggregatedScore score,
            final String prefix, final Set<String> sourcePaths) {
        return score.getCoveredFiles(Metric.LINE).stream()
                .map(file -> createAnnotationsForMissedLines(file, prefix, sourcePaths))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<Annotation> createAnnotationsForMissedLines(final FileNode file,
            final String prefix, final Set<String> sourcePaths) {
        return file.getMissedLineRanges().stream()
                .map(range -> createAnnotationForMissedLineRange(file, range, prefix, sourcePaths))
                .collect(Collectors.toList());
    }

    private Annotation createAnnotationForMissedLineRange(final FileNode file, final LineRange range,
            final String prefix, final Set<String> sourcePaths) {
        var relativePath = createRelativeRepositoryPath(file.getRelativePath(), prefix, sourcePaths);

        return new Annotation(relativePath,
                range.getStart(), range.getEnd(),
                AnnotationLevel.WARNING, getMissedLinesDescription(range))
                .withTitle(getMissedLinesMessage(range));
    }

    private String getMissedLinesMessage(final LineRange range) {
        if (range.getStart() == range.getEnd()) {
            return "Not covered line";
        }
        return "Not covered lines";
    }

    private String getMissedLinesDescription(final LineRange range) {
        if (range.getStart() == range.getEnd()) {
            return String.format("Line %d is not covered by tests", range.getStart());
        }
        return String.format("Lines %d-%d are not covered by tests", range.getStart(), range.getEnd());
    }

    private List<Annotation> createAnnotationsForPartiallyCoveredLines(final AggregatedScore score,
            final String prefix, final Set<String> sourcePaths) {
        return score.getCoveredFiles(Metric.BRANCH).stream()
                .map(file -> createAnnotationsForMissedBranches(file, prefix, sourcePaths))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<Annotation> createAnnotationsForMissedBranches(final FileNode file,
            final String prefix, final Set<String> sourcePaths) {
        return file.getPartiallyCoveredLines().entrySet().stream()
                .map(entry -> createAnnotationForMissedBranches(file, entry, prefix, sourcePaths))
                .collect(Collectors.toList());
    }

    private Annotation createAnnotationForMissedBranches(final FileNode file,
            final Entry<Integer, Integer> branchCoverage,
            final String prefix, final Set<String> sourcePaths) {
        return new Annotation(createRelativeRepositoryPath(file.getRelativePath(), prefix, sourcePaths),
                branchCoverage.getKey(),
                AnnotationLevel.WARNING,
                createBranchMessage(branchCoverage.getKey(), branchCoverage.getValue()))
                .withTitle("Partially covered line");
    }

    private String createBranchMessage(final int line, final int missed) {
        if (missed == 1) {
            return String.format("Line %d is only partially covered, one branch is missing", line);
        }
        return String.format("Line %d is only partially covered, %d branches are missing", line, missed);
    }

    private String createRelativeRepositoryPath(final String fileName,
            final String prefix, final Set<String> sourcePaths) {
        var cleaned = StringUtils.removeStart(fileName, prefix);
        if (Files.exists(Path.of(cleaned))) {
            return cleaned;
        }
        for (String s : sourcePaths) {
            var added = s + "/" + cleaned;
            if (Files.exists(Path.of(added))) {
                return added;
            }
        }
        return cleaned;
    }

    private List<Annotation> createAnnotationsForSurvivedMutations(final AggregatedScore score,
            final String prefix, final Set<String> sourcePaths) {
        return score.getCoveredFiles(Metric.MUTATION).stream()
                .map(file -> createAnnotationsForSurvivedMutations(file, prefix, sourcePaths))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<Annotation> createAnnotationsForSurvivedMutations(final FileNode file,
            final String prefix, final Set<String> sourcePaths) {
        return file.getSurvivedMutationsPerLine().entrySet().stream()
                .map(entry -> createAnnotationForSurvivedMutation(file, entry, prefix, sourcePaths))
                .collect(Collectors.toList());
    }

    private Annotation createAnnotationForSurvivedMutation(final FileNode file,
            final Entry<Integer, List<Mutation>> mutationsPerLine,
            final String prefix, final Set<String> sourcePaths) {
        return new Annotation(createRelativeRepositoryPath(file.getRelativePath(), prefix, sourcePaths),
                mutationsPerLine.getKey(),
                AnnotationLevel.WARNING,
                createMutationMessage(mutationsPerLine.getKey(), mutationsPerLine.getValue()))
                .withTitle("Mutation survived")
                .withRawDetails(createMutationDetails(mutationsPerLine.getValue()));
    }

    private String createMutationMessage(final int line, final List<Mutation> survived) {
        if (survived.size() == 1) {
            return String.format("One mutation survived in line %d (%s)", line, formatMutator(survived));
        }
        return String.format("%d mutations survived in line %d", survived.size(), line);
    }

    private String formatMutator(final List<Mutation> survived) {
        return survived.get(0).getMutator().replaceAll(".*\\.", "");
    }

    private String createMutationDetails(final List<Mutation> mutations) {
        return mutations.stream()
                .map(mutation -> String.format("- %s (%s)", mutation.getDescription(), mutation.getMutator()))
                .collect(Collectors.joining("\n", "Survived mutations:\n", ""));
    }
}
