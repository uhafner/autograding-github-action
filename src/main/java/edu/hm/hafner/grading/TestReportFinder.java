package edu.hm.hafner.grading;

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
