package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TestReportFinder}.
 *
 * @author Ullrich Hafner
 */
class ReportFinderTest {
    @Test
    void shouldFindTestReports() {
        ReportFinder finder = new ReportFinder();

        assertThat(finder.find("src/test/resources/", "glob:**/junit/*.xml")).hasSize(2);
        assertThat(finder.find("src/test/resources/", "glob:src/test/resources/junit/*Not-Passed.xml")).hasSize(1);
        assertThat(finder.find("src/test/resources/", "glob:src/**/junit/*.html")).isEmpty();

        assertThat(finder.find("src/java/", "glob:**/*.xml")).isEmpty();
    }
}
