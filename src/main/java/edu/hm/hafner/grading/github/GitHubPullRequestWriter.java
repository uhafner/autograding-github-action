package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.grading.AggregatedScore;
import edu.hm.hafner.grading.AnalysisScore;
import edu.hm.hafner.grading.CoverageScore;
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
 * Writes a comment in a pull request.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.SystemPrintln")
public class GitHubPullRequestWriter {
    /** Status of the checks result. */
    public enum ChecksStatus {
        SUCCESS,
        ERROR
    }

    /**
     * Writes the specified comment as GitHub checks result. Requires that the environment variables {@code HEAD_SHA},
     * {@code GITHUB_SHA}, {@code GITHUB_REPOSITORY}, and {@code TOKEN} are correctly set.
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
        getEnv("GITHUB_WORKSPACE");
        getEnv("RUNNER_WORKSPACE");

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

        String actualSha = StringUtils.defaultIfBlank(getEnv("HEAD_SHA"), getEnv("GITHUB_SHA"));
        System.out.println(">>>> ACTUAL_SHA: " + actualSha);

        try {
            GitHub github = new GitHubBuilder().withAppInstallationToken(oAuthToken).build();
            GHCheckRunBuilder check = github.getRepository(repository)
                    .createCheckRun(name, actualSha)
                    .withStatus(Status.COMPLETED)
                    .withStartedAt(Date.from(Instant.now()))
                    .withConclusion(status == ChecksStatus.SUCCESS ? Conclusion.SUCCESS : Conclusion.FAILURE);

            Output output = new Output(header, summary).withText(comment);
            handleAnnotations(score, output);

            check.add(output);
            GHCheckRun run = check.create();

            System.out.println("Successfully created check " + run);

            var prNumber = getEnv("PR_NUMBER");
            if (!prNumber.isBlank()) {
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
            var cwd = Path.of(".").toAbsolutePath().toString();
            System.out.println(">>>> CWD: " + cwd);

            createLineAnnotationsForWarnings(score, cwd, output);
            createLineAnnotationsForMissedLines(score, cwd, output);
            createLineAnnotationsForPartiallyCoveredLines(score, cwd, output);
            createLineAnnotationsForSurvivedMutations(score, cwd, output);
        }
    }

    private void createLineAnnotationsForWarnings(final AggregatedScore score, final String prefix,
            final Output output) {
        score.getAnalysisScores().stream()
                .map(AnalysisScore::getReport)
                .flatMap(Report::stream)
                .map(issue -> createAnnotation(prefix, issue))
                .forEach(output::add);
    }

    private Annotation createAnnotation(final String prefix, final Issue issue) {
        Annotation annotation = new Annotation(cleanFileName(prefix, issue.getFileName()),
                issue.getLineStart(), issue.getLineEnd(),
                AnnotationLevel.WARNING, issue.getMessage()).withTitle(issue.getType());
        if (issue.getLineStart() == issue.getLineEnd()) {
            return annotation.withStartColumn(issue.getColumnStart()).withEndColumn(issue.getColumnEnd());
        }
        return annotation;
    }

    private String cleanFileName(final String prefix, final String fileName) {
        return StringUtils.removeStart(fileName, prefix);
    }

    private void createLineAnnotationsForMissedLines(final AggregatedScore score, final String prefix,
            final Output output) {
        score.getCodeCoverageScores().stream()
                .map(CoverageScore::getReport)
                .map(Node::getAllFileNodes)
                .flatMap(Collection::stream)
                .map(file -> createLineCoverageAnnotation(prefix, file))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createLineCoverageAnnotation(final String prefix, final FileNode file) {
        return file.getMissedLineRanges().stream()
                .map(range -> new Annotation(cleanFileName(prefix, file.getName()),
                        range.getStart(), range.getEnd(),
                        AnnotationLevel.WARNING,
                        getMissedLinesDescription(range))
                        .withTitle(getMissedLinesMessage(range)))
                .collect(Collectors.toList());
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

    private void createLineAnnotationsForPartiallyCoveredLines(final AggregatedScore score, final String prefix,
            final Output output) {
        score.getCodeCoverageScores().stream()
                .map(CoverageScore::getReport)
                .map(Node::getAllFileNodes)
                .flatMap(Collection::stream)
                .map(file -> createBranchCoverageAnnotation(prefix, file))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createBranchCoverageAnnotation(final String prefix, final FileNode file) {
        return file.getPartiallyCoveredLines().entrySet().stream()
                .map(entry -> new Annotation(cleanFileName(prefix, file.getName()),
                        entry.getKey(),
                        AnnotationLevel.WARNING,
                        createBranchMessage(entry.getKey(), entry.getValue()))
                        .withTitle("Partially covered line"))
                .collect(Collectors.toList());
    }

    private String createBranchMessage(final int line, final int missed) {
        if (missed == 1) {
            return String.format("Line %d is only partially covered, one branch is missing", line);

        }
        return String.format("Line %d is only partially covered, %d branches are missing", line, missed);
    }

    private void createLineAnnotationsForSurvivedMutations(final AggregatedScore score, final String prefix,
            final Output output) {
        score.getMutationCoverageScores().stream()
                .map(CoverageScore::getReport)
                .map(Node::getAllFileNodes)
                .flatMap(Collection::stream)
                .map(file -> createMutationCoverageAnnotation(prefix, file))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createMutationCoverageAnnotation(final String prefix, final FileNode file) {
        return file.getSurvivedMutationsPerLine().entrySet().stream()
                .map(entry -> new Annotation(cleanFileName(prefix, file.getName()),
                        entry.getKey(),
                        AnnotationLevel.WARNING,
                        createMutationMessage(entry.getKey(), entry.getValue()))
                        .withTitle("Mutation survived")
                        .withRawDetails(createMutationDetails(entry.getValue())))
                .collect(Collectors.toList());
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
