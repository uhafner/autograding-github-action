package edu.hm.hafner.grading;

import java.util.List;
import java.util.stream.Collectors;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.PitAdapter;

/**
 * Provides static analysis scores by converting corresponding {@link Report} instances.
 *
 * @author Ullrich Hafner
 */
class AnalysisReportSupplier extends AnalysisSupplier {
    private final List<AnalysisScore> analysisReports;

    AnalysisReportSupplier(final List<AnalysisScore> analysisReports) {
        this.analysisReports = analysisReports;
    }

    @Override
    protected List<AnalysisScore> createScores(final AnalysisConfiguration configuration) {
        return analysisReports;
    }
}
