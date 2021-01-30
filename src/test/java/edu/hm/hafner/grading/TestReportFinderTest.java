package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link TestReportFinder}.
 *
 * @author Ullrich Hafner
 */
class TestReportFinderTest {
    @Test
    void shouldReadNotPassedTestReport() {
        TestReportFinder finder = new TestReportFinder();
        assertThat(finder.find("glob:./src/test/resources/junit/*Not-Passed.xml")).hasSize(1).element(0).satisfies(
                r -> {
                    assertThat(r.size()).isEqualTo(1);
                    assertThat(r.getCounter(JUnitAdapter.FAILED_TESTS)).isEqualTo(1);
                    assertThat(r.getCounter(JUnitAdapter.PASSED_TESTS)).isEqualTo(3);
                    Issue failure = r.get(0);
                    assertThat(failure.getFileName()).isEqualTo("de/tobiasmichael/me/MyTest.java");
                    assertThat(failure.getMessage()).isEqualTo("testConcatenate4 : org.opentest4j.AssertionFailedError: expected: <onefive> but was: <onetwo>\n"
                            + "\tat de.tobiasmichael.me.MyTest.testConcatenate4(MyTest.java:35)");
                }
        );
    }

    @Test
    void shouldReadPassedTestReport() {
        TestReportFinder finder = new TestReportFinder();
        assertThat(finder.find("glob:./src/test/resources/junit/*Test-Passed.xml")).hasSize(1).element(0).satisfies(
                r -> {
                    assertThat(r.getCounter(JUnitAdapter.FAILED_TESTS)).isEqualTo(0);
                    assertThat(r.getCounter(JUnitAdapter.PASSED_TESTS)).isEqualTo(4);
                }
        );
    }

    @Test
    void shouldReadArchitectureTests() {
        TestReportFinder finder = new TestReportFinder();
        assertThat(finder.find("glob:./src/test/resources/junit/*Aufgabe*.xml")).hasSize(1).element(0).satisfies(
                r -> {
                    assertThat(r.getCounter(JUnitAdapter.FAILED_TESTS)).isEqualTo(12);
                    assertThat(r.getCounter(JUnitAdapter.PASSED_TESTS)).isEqualTo(21);
                }
        );
    }
}
