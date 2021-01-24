package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link GradingConfiguration}.
 *
 * @author Ullrich Hafner
 */
class GradingConfigurationTest {
    @Test
    void shouldReturnDefaultPattern() {
        assertThat(new GradingConfiguration(
                "{ \"tests\": {\"maxScore\":5,\"failureImpact\":1,\"passedImpact\":2,\"skippedImpact\":3}}")
                .getTestPattern()).isEqualTo(GradingConfiguration.SUREFIRE_REPORT_PATTERN);
        assertThat(new GradingConfiguration("{ \"tests\": {}}").getTestPattern()).isEqualTo(
                GradingConfiguration.SUREFIRE_REPORT_PATTERN);

        assertThat(new GradingConfiguration("{}").getTestPattern()).isEqualTo(
                GradingConfiguration.SUREFIRE_REPORT_PATTERN);
        assertThat(new GradingConfiguration("").getTestPattern()).isEqualTo(
                GradingConfiguration.SUREFIRE_REPORT_PATTERN);
        assertThat(new GradingConfiguration("<[+").getTestPattern()).isEqualTo(
                GradingConfiguration.SUREFIRE_REPORT_PATTERN);
        assertThat(new GradingConfiguration("<[+").getTestPattern()).isEqualTo(
                GradingConfiguration.SUREFIRE_REPORT_PATTERN);

        assertThat(new GradingConfiguration("<[+").getAnalysisPattern()).isEqualTo(GradingConfiguration.ALL_FILES);
    }

    @Test
    void shouldReturnProvidedTestPattern() {
        assertThat(new GradingConfiguration(
                "{ \"tests\": {\"pattern\":\"*/*.xml\", \"maxScore\":5,\"failureImpact\":1,\"passedImpact\":2,\"skippedImpact\":3}}")
                .getTestPattern()).isEqualTo("*/*.xml");
    }

    @Test
    void shouldReturnProvidedAnalysisPattern() {
        assertThat(new GradingConfiguration(
                "{\n"
                        + "  \"analysis\": {\n"
                        + "    \"pattern\": \"File.*\",\n"
                        + "    \"maxScore\": 100,\n"
                        + "    \"errorImpact\": -5,\n"
                        + "    \"highImpact\": -3,\n"
                        + "    \"normalImpact\": -2,\n"
                        + "    \"lowImpact\": -1\n"
                        + "  }}")
                .getAnalysisPattern()).isEqualTo("File.*");
    }
}
