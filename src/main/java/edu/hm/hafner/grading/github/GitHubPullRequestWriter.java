package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
import edu.hm.hafner.grading.Configuration;
import edu.hm.hafner.grading.Score;
import edu.hm.hafner.grading.ToolConfiguration;
import edu.hm.hafner.util.LineRange;

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
 * Writes a comment in a pull request and publish the GitHub checks results.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.SystemPrintln")
public class GitHubPullRequestWriter {

    private static final String GITHUB_WORKSPACE_REL = "/github/workspace/./";
    private static final String GITHUB_WORKSPACE_ABS = "/github/workspace/";

    /** Status of the checks result. */
    public enum ChecksStatus {
        SUCCESS,
        ERROR
    }

    /**
     * Writes the specified comment as GitHub checks result. Requires that the environment variables {@code GITHUB_SHA},
     * {@code GITHUB_REPOSITORY}, and {@code GITHUB_TOKEN} are correctly set.
     *
     * @param name
     *         the name of the checks result
     * @param score
     *         the score to write
     * @param header
     *         the header of the check
     * @param summary
     *         the summary of the check
     * @param comment
     *         the details of the check (supports Markdown)
     * @param prComment
     *         the comment to write in the pull request
     * @param status
     *         the status of the checks result
     */
    public void addComment(final String name, final AggregatedScore score,
            final String header, final String summary, final String comment, final String prComment,
            final ChecksStatus status) {
        var repository = getEnv("GITHUB_REPOSITORY");
        if (repository.isBlank()) {
            System.out.println("No GITHUB_REPOSITORY defined - skipping");

            return;
        }
        String oAuthToken = getEnv("GITHUB_TOKEN");
        if (oAuthToken.isBlank()) {
            System.out.println("No valid GITHUB_TOKEN found - skipping");

            return;
        }

        String sha = getEnv("GITHUB_SHA");

        try {
            GitHub github = new GitHubBuilder().withAppInstallationToken(oAuthToken).build();
            GHCheckRunBuilder check = github.getRepository(repository)
                    .createCheckRun(name, sha)
                    .withStatus(Status.COMPLETED)
                    .withStartedAt(Date.from(Instant.now()))
                    .withConclusion(status == ChecksStatus.SUCCESS ? Conclusion.SUCCESS : Conclusion.FAILURE);

            Output output = new Output(header, summary).withText(comment);
            handleAnnotations(score, output);

            check.add(output);
            GHCheckRun run = check.create();

            System.out.println("Successfully created check " + run);

            var prNumber = getEnv("PR_NUMBER");
            if (!prNumber.isBlank()) { // optional PR comment
                github.getRepository(repository)
                        .getPullRequest(Integer.parseInt(prNumber))
                        .comment(prComment + addCheckLink(run));
                System.out.println("Successfully commented PR#" + prNumber);
            }
        }
        catch (IOException exception) {
            System.out.println("Could not create check due to " + exception);
        }
    }

    private String addCheckLink(final GHCheckRun run) {
        return String.format("## %n%n More details are available in the [GitHub Checks Result](%s).%n",
                run.getDetailsUrl().toString());
    }

    private String getEnv(final String key) {
        String value = StringUtils.defaultString(System.getenv(key));
        System.out.println(">>>> " + key + ": " + value);
        return value;
    }

    private void handleAnnotations(final AggregatedScore score, final Output output) {
        if (getEnv("SKIP_ANNOTATIONS").isEmpty()) {
            var prefix = computeAbsolutePathPrefixToRemove();

            var additionalAnalysisSourcePaths = extractAdditionalSourcePaths(score.getAnalysisScores());
            createAnnotationsForIssues(score, prefix, additionalAnalysisSourcePaths, output);

            var additionalCoverageSourcePaths = extractAdditionalSourcePaths(score.getCodeCoverageScores());
            createAnnotationsForMissedLines(score, prefix, additionalCoverageSourcePaths, output);
            createAnnotationsForPartiallyCoveredLines(score, prefix, additionalCoverageSourcePaths, output);

            var additionalMutationSourcePaths = extractAdditionalSourcePaths(score.getMutationCoverageScores());
            createAnnotationsForSurvivedMutations(score, prefix, additionalMutationSourcePaths, output);
        }
    }

    private String computeAbsolutePathPrefixToRemove() {
        return String.format("%s/%s/", getEnv("RUNNER_WORKSPACE"),
                StringUtils.substringAfter(getEnv("GITHUB_REPOSITORY"), "/"));
    }

    private Set<String> extractAdditionalSourcePaths(final List<? extends Score<?, ?>> scores) {
        return scores.stream()
                .map(Score::getConfiguration)
                .map(Configuration::getTools)
                .flatMap(Collection::stream)
                .map(ToolConfiguration::getSourcePath).collect(Collectors.toSet());
    }

    private void createAnnotationsForIssues(final AggregatedScore score, final String prefix,
            final Set<String> sourcePaths, final Output output) {
        score.getIssues().stream()
                .map(issue -> createAnnotationForIssue(issue, prefix, sourcePaths))
                .forEach(output::add);
    }

    private Annotation createAnnotationForIssue(final Issue issue, final String prefix,
            final Set<String> sourcePaths) {
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

    private void createAnnotationsForMissedLines(final AggregatedScore score, final String prefix,
            final Set<String> sourcePaths, final Output output) {
        score.getCoveredFiles(Metric.LINE).stream()
                .map(file -> createAnnotationsForMissedLines(file, prefix, sourcePaths))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createAnnotationsForMissedLines(final FileNode file, final String prefix,
            final Set<String> sourcePaths) {
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

    private void createAnnotationsForPartiallyCoveredLines(final AggregatedScore score, final String prefix,
            final Set<String> sourcePaths, final Output output) {
        score.getCoveredFiles(Metric.BRANCH).stream()
                .map(file -> createAnnotationsForMissedBranches(file, prefix, sourcePaths))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createAnnotationsForMissedBranches(final FileNode file, final String prefix,
            final Set<String> sourcePaths) {
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

    private String createRelativeRepositoryPath(final String fileName, final String prefix,
            final Set<String> sourcePaths) {
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

    private void createAnnotationsForSurvivedMutations(final AggregatedScore score, final String prefix,
            final Set<String> sourcePaths, final Output output) {
        score.getCoveredFiles(Metric.MUTATION).stream()
                .map(file -> createAnnotationsForSurvivedMutations(file, prefix, sourcePaths))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createAnnotationsForSurvivedMutations(final FileNode file, final String prefix,
            final Set<String> sourcePaths) {
        return file.getSurvivedMutationsPerLine().entrySet().stream()
                .map(entry -> createAnnotationForSurvivedMutation(file, entry, prefix, sourcePaths))
                .collect(Collectors.toList());
    }

    private Annotation createAnnotationForSurvivedMutation(final FileNode file,
            final Entry<Integer, List<Mutation>> mutationsPerLine, final String prefix,
            final Set<String> sourcePaths) {
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
