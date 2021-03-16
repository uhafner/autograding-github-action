package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.violations.PitAdapter;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link PitReportFinder}.
 *
 * @author Ullrich Hafner
 */
class PitReportFinderTest {
    @Test
    void shouldReportMutations() {
        ReportFinder finder = new ReportFinder();

        List<Path> pitReports = finder.find("src/test/resources/", "glob:**/pit/*.xml");
        assertThat(pitReports).hasSize(1);

        List<Report> reports = new PitReportFinder().parseFiles(pitReports);
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getCounter(PitAdapter.TOTAL_MUTATIONS)).isEqualTo(191);
        assertThat(reports.get(0).getCounter(PitAdapter.KILLED_MUTATIONS)).isEqualTo(139);
        assertThat(reports.get(0).getCounter(PitAdapter.SURVIVED_MUTATIONS)).isEqualTo(20);
        assertThat(reports.get(0).getCounter(PitAdapter.UNCOVERED_MUTATIONS)).isEqualTo(32);

        AggregatedScore score = new AggregatedScore("{\"pit\":"
                + "{\"maxScore\":100,"
                + "\"undetectedImpact\":0,"
                + "\"detectedImpact\":0,"
                + "\"undetectedPercentageImpact\":-1,"
                + "\"detectedPercentageImpact\":0}}");
        score.addPitScores(new PitReportSupplier(reports));

        assertThat(score.getPitAchieved()).isEqualTo(73);
        assertThat(score.getPitRatio()).isEqualTo(73);
        assertThat(score.getPitScores()).hasSize(1);
        assertThat(score.getPitScores().get(0).getMutationsSize()).isEqualTo(191);
        assertThat(score.getPitScores().get(0).getUndetectedSize()).isEqualTo(52);
    }
}
