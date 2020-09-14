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
import edu.hm.hafner.analysis.parser.violations.PitAdapter;

/**
 * Finds JUnit test reports in the file system.
 *
 * @author Ullrich Hafner
 */
public class PitReportFinder extends ReportFinder {
    private static final String PIT_REPORT_PATTERN = "target/pit-reports/";

    /**
     * Finds all PIT result files of the pattern {@link #PIT_REPORT_PATTERN}.
     *
     * @return the PIT result files
     */
    public List<Report> find() {
        List<Path> reportFiles = getPaths(PIT_REPORT_PATTERN);
        if (reportFiles.size() == 0) {
            System.out.println("No PIT result files found!");

            return Collections.emptyList();
        }

        System.out.println("Reading PIT results: ");
        System.out.println(reportFiles);

        PitAdapter parser = new PitAdapter();
        return reportFiles.stream()
                .map(FileReaderFactory::new)
                .map(parser::parse)
                .collect(Collectors.toList());
    }
}
