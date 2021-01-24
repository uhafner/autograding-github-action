package edu.hm.hafner.grading;

import java.nio.file.FileSystem;
import java.nio.file.Path;
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
class TestReportFinder extends ReportFinder {
    /**
     * Finds and parses all JUnit result files that match the specified pattern.
     *
     * @param pattern
     *         the file name pattern
     *
     * @return the JUnit result files
     * @see FileSystem#getPathMatcher(String)
     */
    public List<Report> find(final String pattern) {
        List<Path> reportFiles = find(".", pattern);
        if (reportFiles.size() == 0) {
            System.out.format("[ERROR] No JUnit result files found for pattern '%s'%n", pattern);

            return Collections.emptyList();
        }

        Collections.sort(reportFiles);
        System.out.println("Reading test results: ");
        System.out.println(reportFiles);

        JUnitAdapter parser = new JUnitAdapter();
        return reportFiles.stream()
                .map(FileReaderFactory::new)
                .map(parser::parseFile)
                .collect(Collectors.toList());
    }
}
