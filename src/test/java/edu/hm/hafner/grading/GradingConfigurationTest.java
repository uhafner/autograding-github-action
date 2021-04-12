package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

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
                        + "    \"maxScore\": 100,\n"
                        + "    \"errorImpact\": -5,\n"
                        + "    \"highImpact\": -3,\n"
                        + "    \"normalImpact\": -2,\n"
                        + "    \"lowImpact\": -1\n"
                        + "  }}")
                .getAnalysisPattern()).isEqualTo("File.*");
    }

}
