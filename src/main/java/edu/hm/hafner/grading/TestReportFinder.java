package edu.hm.hafner.grading;

import java.io.IOException;
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

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;

/**
 * Finds JUnit test reports in the file system.
 *
 * @author Ullrich Hafner
 */
public class TestReportFinder {
    private static final String SUREFIRE_REPORT_PATTERN = "target/surefire-reports/";

    /**
     * Finds all JUnit result files of the pattern {@link #SUREFIRE_REPORT_PATTERN}.
     *
     * @return the JUnit result files
     */
    public List<Report> find() {
        List<Path> reportFiles = getPaths(SUREFIRE_REPORT_PATTERN);
        if (reportFiles.size() == 0) {
            System.out.println("No JUnit result files found!");

            return Collections.emptyList();
        }

        System.out.println("Reading test results: ");
        System.out.println(reportFiles);

        JUnitAdapter parser = new JUnitAdapter();
        return reportFiles.stream()
                .map(FileReaderFactory::new)
                .map(parser::parse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of paths that matches the glob pattern.
     *
     * @param location
     *         path where to search for files
     *
     * @return list with paths
     */
    private static List<Path> getPaths(final String location) {
        try {
            PathSimpleFileVisitor visitor = new PathSimpleFileVisitor();
            Files.walkFileTree(Paths.get(location), visitor);
            return visitor.getMatches();
        }
        catch (IOException exception) {
            System.out.println("Cannot find files due to " + exception);

            return new ArrayList<>();
        }
    }

    private static class PathSimpleFileVisitor extends SimpleFileVisitor<Path> {
        private final PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.xml");
        private final List<Path> matches = new ArrayList<>();

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
