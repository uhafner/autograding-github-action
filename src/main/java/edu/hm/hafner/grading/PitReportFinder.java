package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.PitAdapter;

/**
 * Provides PIT mutation coverage scores by converting corresponding {@link Report} instances.
 */
class PitReportFinder extends ReportFinder {
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
                .map(parser::parseFile)
                .collect(Collectors.toList());
    }
}
