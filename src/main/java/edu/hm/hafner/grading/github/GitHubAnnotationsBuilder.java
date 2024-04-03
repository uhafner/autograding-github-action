package edu.hm.hafner.grading.github;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.grading.CommentBuilder;
import edu.hm.hafner.util.FilteredLog;

import org.kohsuke.github.GHCheckRun.AnnotationLevel;
import org.kohsuke.github.GHCheckRunBuilder.Annotation;
import org.kohsuke.github.GHCheckRunBuilder.Output;

/**
 * Creates GitHub annotations for static analysis warnings, for lines with missing coverage, and for lines with
 * survived mutations.
 *
 * @author Ullrich Hafner
 */
class GitHubAnnotationsBuilder extends CommentBuilder {
    private static final String GITHUB_WORKSPACE_REL = "/github/workspace/./";
    private static final String GITHUB_WORKSPACE_ABS = "/github/workspace/";

    private final Output output;
    private final FilteredLog log;
    private final int maxWarningComments;
    private final int maxCoverageComments;

    GitHubAnnotationsBuilder(final Output output, final String prefix, final FilteredLog log) {
        super(prefix, GITHUB_WORKSPACE_REL, GITHUB_WORKSPACE_ABS);

        this.output = output;
        this.log = log;

        maxWarningComments = getIntegerEnvironment("MAX_WARNING_ANNOTATIONS");
        log.logInfo(">>>> MAX_WARNING_ANNOTATIONS: %d", getMaxWarningComments());

        maxCoverageComments = getIntegerEnvironment("MAX_COVERAGE_ANNOTATIONS");
        log.logInfo(">>>> MAX_COVERAGE_ANNOTATIONS: %d", getMaxCoverageComments());
    }

    @Override
    protected int getMaxWarningComments() {
        return maxWarningComments;
    }

    @Override
    protected int getMaxCoverageComments() {
        return maxCoverageComments;
    }

    private int getIntegerEnvironment(final String key) {
        try {
            return Integer.parseInt(getEnv(key));
        }
        catch (NumberFormatException exception) {
            log.logError(">>>> Error: no integer value");

            return Integer.MAX_VALUE;
        }
    }

    private String getEnv(final String name) {
        return StringUtils.defaultString(System.getenv(name));
    }

    @Override
    @SuppressWarnings("checkstyle:ParameterNumber")
    protected void createComment(final CommentType commentType, final String relativePath,
            final int lineStart, final int lineEnd,
            final String message, final String title,
            final int columnStart, final int columnEnd,
            final String details, final String markDownDetails) {
        Annotation annotation = new Annotation(relativePath,
                lineStart, lineEnd, AnnotationLevel.WARNING, message).withTitle(title);

        if (lineStart == lineEnd) {
            annotation.withStartColumn(columnStart).withEndColumn(columnEnd);
        }
        if (StringUtils.isNotBlank(details)) {
            annotation.withRawDetails(details);
        }

        output.add(annotation);
    }
}
