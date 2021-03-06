package edu.hm.hafner.grading;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.util.VisibleForTesting;

/**
 * Base class that finds files in the workspace and parses these files with a parser that returns a {@link Report}
 * instance.
 *
 * @author Ullrich Hafner
 */
class ReportFinder {
    private final String repository;
    private final String branch;

    ReportFinder() {
        this(StringUtils.defaultString(System.getenv("GITHUB_REPOSITORY")),
                StringUtils.remove(StringUtils.defaultString(System.getenv("GITHUB_REF")), "refs/heads/"));
    }

    @VisibleForTesting
    ReportFinder(final String repository, final String branch) {
        this.repository = repository;
        this.branch = branch;
    }

    /**
     * Returns the paths that match the specified pattern.
     *
     * @param directory
     *         the directory where to search for files
     * @param pattern
     *         the pattern to use when searching
     *
     * @return the matching paths
     * @see FileSystem#getPathMatcher(String)
     */
    protected List<Path> find(final String directory, final String pattern) {
        try {
            PathMatcherFileVisitor visitor = new PathMatcherFileVisitor(pattern);
            Files.walkFileTree(Paths.get(directory), visitor);
            return visitor.getMatches();
        }
        catch (IOException exception) {
            System.out.println("Cannot find files due to " + exception);

            return new ArrayList<>();
        }
    }

    public String renderLinks(final String directory, final String pattern) {
        String result = "### Analyzed files\n\n";
        return find(directory, pattern).stream()
                .map(file -> String.format("- [%s](https://github.com/%s/blob/%s/%s)",
                        StringUtils.substringAfterLast(file.toString(), "/"),
                        repository,
                        branch,
                        file)).collect(Collectors.joining("\n", result, "\n"));
    }

    private static class PathMatcherFileVisitor extends SimpleFileVisitor<Path> {
        private final PathMatcher pathMatcher;
        private final List<Path> matches = new ArrayList<>();

        PathMatcherFileVisitor(final String syntaxAndPattern) {
            try {
                pathMatcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
            }
            catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Pattern not valid for FileSystem.getPathMatcher: " + syntaxAndPattern);
            }
        }

        List<Path> getMatches() {
            return matches;
        }

        @Override
        public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) {
            if (pathMatcher.matches(path)) {
                matches.add(path);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(final Path file, final IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
