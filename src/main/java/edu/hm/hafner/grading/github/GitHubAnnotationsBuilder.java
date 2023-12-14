package edu.hm.hafner.grading.github;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.grading.CommentBuilder;

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

    GitHubAnnotationsBuilder(final Output output, final String prefix) {
        super(prefix, GITHUB_WORKSPACE_REL, GITHUB_WORKSPACE_ABS);

        this.output = output;
    }

    @Override
    @SuppressWarnings("checkstyle:ParameterNumber")
    protected void createComment(final CommentType commentType, final String relativePath,
            final int lineStart, final int lineEnd,
            final String message, final String title,
            final int columnStart, final int columnEnd,
            final String details) {
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
