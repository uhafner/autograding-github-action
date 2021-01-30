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

        assertThat(finder.find("src/test/resources/", "glob:**/junit/*.xml")).hasSize(3);
        assertThat(finder.find("src/test/resources/", "glob:src/test/resources/junit/*Not-Passed.xml")).hasSize(1);
        assertThat(finder.find("src/test/resources/", "glob:src/**/junit/*.html")).isEmpty();

        assertThat(finder.find("src/java/", "glob:**/*.xml")).isEmpty();
    }

    @Test
    void shouldFindSources() {
        ReportFinder finder = new ReportFinder();

        assertThat(finder.find("src/main/java/", "regex:.*Jacoco.*\\.java")).hasSize(3);
    }

    @Test
    void shouldCreateAffectedFilesReport() {
        ReportFinder finder = new ReportFinder();

        assertThat(finder.renderLinks("src/main/java/", "regex:.*Jacoco.*\\.java"))
                .contains("# Analyzed files",
                        "- [JacocoParser.java](https://github.com//blob//src/main/java/de/tobiasmichael/me/Util/JacocoParser.java)",
                        "- [JacocoCounter.java](https://github.com//blob//src/main/java/de/tobiasmichael/me/Util/JacocoCounter.java)",
                        "- [JacocoReport.java](https://github.com//blob//src/main/java/de/tobiasmichael/me/Util/JacocoReport.java)");

    }
}
