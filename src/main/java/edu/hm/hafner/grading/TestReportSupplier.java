package edu.hm.hafner.grading;

import java.util.List;
import java.util.stream.Collectors;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;

/**
 * Provides test scores by converting corresponding {@link Report} instances.
 *
 * @author Ullrich Hafner
 */
public class TestReportSupplier extends TestSupplier {
    private final List<Report> testReports;

    /**
     * Creates a new instance of {@link TestReportSupplier}.
     *
     * @param testReports
     *         the JUnit test reports
     */
    public TestReportSupplier(final List<Report> testReports) {
        this.testReports = testReports;
    }

    @Override
    protected List<TestScore> createScores(final TestConfiguration configuration) {
        return testReports.stream()
                .map(report -> createTestScore(configuration, report))
                .collect(Collectors.toList());
    }

    private TestScore createTestScore(final TestConfiguration configuration, final Report report) {
        return new TestScore.TestScoreBuilder()
                .withConfiguration(configuration)
                .withDisplayName("JUnit")
                .withTotalSize(report.getCounter(JUnitAdapter.TOTAL_TESTS))
                .withFailedSize(report.getCounter(JUnitAdapter.FAILED_TESTS))
                .build();
    }
}
