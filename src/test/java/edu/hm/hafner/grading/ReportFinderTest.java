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
        ReportFinder finder = new ReportFinder();

        assertThat(finder.find("src/test/resources/", "glob:**/junit/*.xml")).hasSize(3);
        assertThat(finder.find("src/test/resources/", "glob:src/test/resources/junit/*Not-Passed.xml")).hasSize(1);
        assertThat(finder.find("src/test/resources/", "glob:src/**/junit/*.html")).isEmpty();

        assertThat(finder.find("src/java/", "glob:**/*.xml")).isEmpty();
    }

    @Test
    void shouldFindSources() {
        ReportFinder finder = new ReportFinder();

        assertThat(finder.find("src/main/java/", "regex:.*Console.*\\.java")).hasSize(3);
    }

    @Test
    void shouldCreateAffectedFilesReport() {
        ReportFinder finder = new ReportFinder("uhafner/autograding-github-action", "master");

        assertThat(finder.renderLinks("src/main/java/", "regex:.*Console.*\\.java"))
                .contains("# Analyzed files",
                        "- [ConsoleCoverageReportFactory.java](https://github.com/uhafner/autograding-github-action/blob/master/src/main/java/edu/hm/hafner/grading/ConsoleCoverageReportFactory.java)",
                        "- [ConsoleAnalysisReportFactory.java](https://github.com/uhafner/autograding-github-action/blob/master/src/main/java/edu/hm/hafner/grading/ConsoleAnalysisReportFactory.java)",
                        "- [ConsoleTestReportFactory.java](https://github.com/uhafner/autograding-github-action/blob/master/src/main/java/edu/hm/hafner/grading/ConsoleTestReportFactory.java)");

    }
}
