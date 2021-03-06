package edu.hm.hafner.grading;

import java.util.ArrayList;
import java.util.List;

import de.tobiasmichael.me.Util.JacocoReport;

/**
 * Provides code coverage scores by converting corresponding {@link JacocoReport} instances.
 *
 * @author Ullrich Hafner
 */
class CoverageReportSupplier extends CoverageSupplier {
    private final JacocoReport coverageReport;

    CoverageReportSupplier(final JacocoReport coverageReport) {
        this.coverageReport = coverageReport;
    }

    @Override
    protected List<CoverageScore> createScores(final CoverageConfiguration configuration) {
        List<CoverageScore> coverageScoreList = new ArrayList<>();
        coverageScoreList.add(createCoverageScore(configuration, "Branch", "branch",
                coverageReport.getBranchPercentage()));
        coverageScoreList.add(createCoverageScore(configuration, "Line", "line",
                coverageReport.getLinePercentage()));
        return coverageScoreList;
    }

    private CoverageScore createCoverageScore(final CoverageConfiguration configuration, final String displayName, final String id,
            final int percentage) {
        return new CoverageScore.CoverageScoreBuilder()
                .withConfiguration(configuration)
                .withDisplayName(displayName)
                .withId(id)
                .withCoveredPercentage(percentage)
                .build();
    }
}
