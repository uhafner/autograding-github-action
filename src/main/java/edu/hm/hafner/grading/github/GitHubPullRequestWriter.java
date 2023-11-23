package edu.hm.hafner.grading.github;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
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
            createLineAnnotationsForWarnings(score, prefix, output);
            createLineAnnotationsForMissedLines(score, prefix, output);
            createLineAnnotationsForPartiallyCoveredLines(score, prefix, output);
            createLineAnnotationsForSurvivedMutations(score, prefix, output);
        }
    }

    private void createLineAnnotationsForWarnings(final AggregatedScore score, final Pattern prefix, final Output output) {
        score.getAnalysisScores().stream()
                .map(AnalysisScore::getReport)
                .flatMap(Report::stream)
                .map(issue -> createAnnotation(prefix, issue))
                .forEach(output::add);
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

    private void createLineAnnotationsForMissedLines(final AggregatedScore score, final Pattern prefix, final Output output) {
        score.getCodeCoverageScores().stream()
                .map(CoverageScore::getReport)
                .map(Node::getAllFileNodes)
                .flatMap(Collection::stream)
                .map(file -> createLineCoverageAnnotation(prefix, file))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createLineCoverageAnnotation(final Pattern prefix, final FileNode file) {
        return file.getMissedLineRanges().stream()
                .map(range -> new Annotation(prefix.matcher(file.getName()).replaceAll(""),
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

    private void createLineAnnotationsForPartiallyCoveredLines(final AggregatedScore score, final Pattern prefix, final Output output) {
        score.getCodeCoverageScores().stream()
                .map(CoverageScore::getReport)
                .map(Node::getAllFileNodes)
                .flatMap(Collection::stream)
                .map(file -> createBranchCoverageAnnotation(prefix, file))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createBranchCoverageAnnotation(final Pattern prefix, final FileNode file) {
        return file.getPartiallyCoveredLines().entrySet().stream()
                .map(entry -> new Annotation(prefix.matcher(file.getName()).replaceAll(""),
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

    private void createLineAnnotationsForSurvivedMutations(final AggregatedScore score, final Pattern prefix, final Output output) {
        score.getMutationCoverageScores().stream()
                .map(CoverageScore::getReport)
                .map(Node::getAllFileNodes)
                .flatMap(Collection::stream)
                .map(file -> createMutationCoverageAnnotation(prefix, file))
                .flatMap(Collection::stream)
                .forEach(output::add);
    }

    private List<Annotation> createMutationCoverageAnnotation(final Pattern prefix, final FileNode file) {
        return file.getSurvivedMutationsPerLine().entrySet().stream()
                .map(entry -> new Annotation(prefix.matcher(file.getName()).replaceAll(""),
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

    private String getEnv(final String env) {
        return StringUtils.defaultString(System.getenv(env));
    }
}
