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

import edu.hm.hafner.analysis.Report;

/**
 * Base class that finds files in the workspace and parses these files with a parser that returns a {@link Report}
 * instance.
 *
 * @author Ullrich Hafner
 */
class ReportFinder {
    /**
     * Returns a list of paths that matches the glob pattern.
     *
     * @param location
     *         path where to search for files
     *
     * @return list with paths
     */
    protected static List<Path> getPaths(final String location) {
        try {
            PathMatcherFileVisitor visitor = new PathMatcherFileVisitor();
            Files.walkFileTree(Paths.get(location), visitor);
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


    private static class PathMatcherFileVisitor extends SimpleFileVisitor<Path> {
        private final PathMatcher pathMatcher;
        private final List<Path> matches = new ArrayList<>();

        PathMatcherFileVisitor() {
            this("glob:**/*.xml");
        }

        PathMatcherFileVisitor(final String syntaxAndPattern) {
            try {
                this.pathMatcher = FileSystems.getDefault().getPathMatcher(syntaxAndPattern);
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
