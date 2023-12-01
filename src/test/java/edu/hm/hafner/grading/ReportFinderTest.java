package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link ReportFinder}.
 *
 * @author Ullrich Hafner
 */
class ReportFinderTest {
    @Test
    void shouldFindTestReports() {
        var finder = new ReportFinder();

        assertThat(finder.find("glob:**/junit/*.xml", "src/test/resources/")).hasSize(3);
        assertThat(finder.find("glob:src/test/resources/junit/*Not-Passed.xml", "src/test/resources/")).hasSize(1);
        assertThat(finder.find("glob:src/**/junit/*.html", "src/test/resources/")).isEmpty();

        assertThat(finder.find("glob:**/*.xml", "src/java/")).isEmpty();
    }

    @Test
    void shouldFindSources() {
        var finder = new ReportFinder();

        assertThat(finder.find("regex:.*Console.*\\.java", "src/main/java/")).hasSize(3);
    }

    @Test
    void shouldCreateAffectedFilesReport() {
        var finder = new ReportFinder("uhafner/autograding-github-action", "master");

        assertThat(finder.renderLinks("src/main/java/", "regex:.*Console.*\\.java"))
                .contains("# Analyzed files",
                        "- [ConsoleCoverageReportFactory.java](https://github.com/uhafner/autograding-github-action/blob/master/src/main/java/edu/hm/hafner/grading/ConsoleCoverageReportFactory.java)",
                        "- [ConsoleAnalysisReportFactory.java](https://github.com/uhafner/autograding-github-action/blob/master/src/main/java/edu/hm/hafner/grading/ConsoleAnalysisReportFactory.java)",
                        "- [ConsoleTestReportFactory.java](https://github.com/uhafner/autograding-github-action/blob/master/src/main/java/edu/hm/hafner/grading/ConsoleTestReportFactory.java)");

    }
}
