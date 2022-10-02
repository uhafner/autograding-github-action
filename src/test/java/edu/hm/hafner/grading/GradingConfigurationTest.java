package edu.hm.hafner.grading;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.checkstyle.CheckStyleParser;
import edu.hm.hafner.analysis.parser.pmd.PmdParser;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link GradingConfiguration}.
 *
 * @author Ullrich Hafner
 */
class GradingConfigurationTest {
    private static final String DEFAULT_PATTERN = "glob:" + GradingConfiguration.SUREFIRE_DEFAULT_PATTERN;

    @Test
    void shouldReturnDefaultPattern() {
        assertThat(new GradingConfiguration(
                "{ \"tests\": {\"maxScore\":5,\"failureImpact\":1,\"passedImpact\":2,\"skippedImpact\":3}}")
                .getTestPattern()).isEqualTo(DEFAULT_PATTERN);
        assertThat(new GradingConfiguration("{ \"tests\": {}}").getTestPattern()).isEqualTo(
                DEFAULT_PATTERN);

        assertThat(new GradingConfiguration("{}").getTestPattern()).isEqualTo(
                DEFAULT_PATTERN);
        assertThat(new GradingConfiguration("").getTestPattern()).isEqualTo(
                DEFAULT_PATTERN);
        assertThat(new GradingConfiguration("<[+").getTestPattern()).isEqualTo(
                DEFAULT_PATTERN);
        assertThat(new GradingConfiguration("<[+").getTestPattern()).isEqualTo(
                DEFAULT_PATTERN);

        assertThat(new GradingConfiguration("<[+").getAnalysisPattern()).isEqualTo(GradingConfiguration.INCLUDE_ALL_FILES);
    }

    @Test
    void shouldReturnProvidedTestPattern() {
        assertThat(new GradingConfiguration(
                "{ \"tests\": {\"pattern\":\"*/*.xml\", \"maxScore\":5,\"failureImpact\":1,\"passedImpact\":2,\"skippedImpact\":3}}")
                .getTestPattern()).isEqualTo("glob:*/*.xml");
    }

    @Test
    void shouldReturnProvidedAnalysisPattern() {
        assertThat(new GradingConfiguration("{\n"
                        + "  \"analysis\": {\n"
                        + "    \"fileFilter\": \"File.*\",\n"
                        + "    \"maxScore\": 100\n"
                        + "  }}")
                .getAnalysisPattern()).isEqualTo("File.*");
    }

    @Test
    void shouldIgnoreSomeTypes() {
        GradingConfiguration gradingConfiguration = new GradingConfiguration("{\n"
                + "  \"analysis\": {\n"
                + "    \"typesIgnorePattern\": \"HideUtilityClassConstructorCheck|UseUtilityClass|UseVarargs\",\n"
                + "    \"maxScore\": 100\n"
                + "  }}");
        assertThat(gradingConfiguration.getTypesIgnorePattern()).isEqualTo("HideUtilityClassConstructorCheck|UseUtilityClass|UseVarargs");

        Report checkstyle = new CheckStyleParser().parse(new FileReaderFactory(
                Paths.get(getClass().getResource("/checkstyle/checkstyle-ignores.xml").getPath())));
        assertThat(checkstyle.size()).isEqualTo(18);

        Report checkstyleFiltered = new AutoGradingAction().filterAnalysisReport(checkstyle, gradingConfiguration);
        assertThat(checkstyleFiltered.size()).isEqualTo(1);

        Report pmd = new PmdParser().parse(new FileReaderFactory(
                Paths.get(getClass().getResource("/pmd/pmd-ignores.xml").getPath())));
        assertThat(pmd.size()).isEqualTo(40);

        Report pmdFiltered = new AutoGradingAction().filterAnalysisReport(pmd, gradingConfiguration);
        assertThat(pmdFiltered.size()).isEqualTo(21);
    }
}
