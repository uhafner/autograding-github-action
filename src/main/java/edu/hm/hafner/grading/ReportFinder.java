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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.VisibleForTesting;

/**
 * Base class that finds files in the workspace.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.SystemPrintln")
class ReportFinder {
    private static String getEnv(final String name) {
        return StringUtils.defaultString(System.getenv(name));
    }

    private final String repository;
    private final String branch;

    ReportFinder() {
        this(getEnv("GITHUB_REPOSITORY"), StringUtils.remove(getEnv("GITHUB_REF"), "refs/heads/"));
    }

    @VisibleForTesting
    ReportFinder(final String repository, final String branch) {
        this.repository = repository;
        this.branch = branch;
    }

    /**
     * Returns the paths for the specified tool.
     *
     * @param tool
     *         the tool to find the reports for
     * @param log
     *         logger
     *
     * @return the paths
     */
    public List<Path> find(final ToolConfiguration tool, final FilteredLog log) {
        log.logInfo("Searching for %s results matching file name pattern %s",
                tool.getDisplayName(), tool.getPattern());
        List<Path> files = new ReportFinder().find("glob:" + tool.getPattern());

        if (files.isEmpty()) {
            log.logError("No matching report files found when using pattern '%s'! "
                    + "Configuration error for '%s'?", tool.getPattern(), tool.getDisplayName());
        }

        Collections.sort(files);
        return files;
    }

    /**
     * Returns the paths that match the specified pattern.
     *
     * @param pattern
     *         the pattern to use when searching
     * @param directory
     *         the directory where to search for files
     *
     * @return the matching paths
     * @see FileSystem#getPathMatcher(String)
     */
    public List<Path> find(final String pattern, final String directory) {
        try {
            var visitor = new PathMatcherFileVisitor(pattern);
            Files.walkFileTree(Paths.get(directory), visitor);
            return visitor.getMatches();
        }
        catch (IOException exception) {
            System.out.println("Cannot find files due to " + exception);

            return new ArrayList<>();
        }
    }

    /**
     * Returns the paths that match the specified pattern.
     *
     * @param pattern
     *         the pattern to use when searching
     *
     * @return the matching paths
     * @see FileSystem#getPathMatcher(String)
     */
    public List<Path> find(final String pattern) {
        return find(pattern, ".");
    }

    public String renderLinks(final String directory, final String pattern) {
        String result = "### Analyzed files\n\n";
        return find(pattern, directory).stream()
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
            super();

            try {
                pathMatcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
            }
            catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Pattern not valid for FileSystem.getPathMatcher: " + syntaxAndPattern, exception);
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
