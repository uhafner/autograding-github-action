package edu.hm.hafner.grading;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.grading.AnalysisToolsConfiguration.ToolConfiguration;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link AnalysisToolsConfiguration}.
 *
 * @author Ullrich Hafner
 */
class AnalysisToolsConfigurationTest {
    @Test
    void shouldReturn2ToolsForDefaultConfiguration() {
        String jsonConfiguration = """
                {
                  "analysis": {
                    "tools": [{
                      "id": "checkstyle",
                      "pattern": "target/checkstyle.xml"
                    },
                      {
                        "id": "spotbugs",
                        "pattern": "target/spotbugsXml.xml"
                      }],
                    "maxScore": 111,
                    "errorImpact": -5,
                    "highImpact": -3,
                    "normalImpact": -2,
                    "lowImpact": -1
                  },
                  "tests": {
                    "maxScore": 100,
                    "passedImpact": 0,
                    "failureImpact": -5,
                    "skippedImpact": -1
                  },
                  "coverage": {
                    "maxScore": 100,
                    "coveredPercentageImpact": 0,
                    "missedPercentageImpact": -1
                  },
                  "pit": {
                    "maxScore": 100,
                    "detectedImpact": 0,
                    "undetectedImpact": 0,
                    "detectedPercentageImpact": 0,
                    "undetectedPercentageImpact": -1
                  }
                }
                """;
        ToolConfiguration[] tools = new AnalysisToolsConfiguration().getTools(jsonConfiguration);

        assertThat(tools).hasSize(2).satisfiesExactly(
                toolConfiguration -> {
                    assertThat(toolConfiguration.getDescriptor().getId()).isEqualTo("checkstyle");
                    assertThat(toolConfiguration.getPattern()).isEqualTo("target/checkstyle.xml");
                },
                toolConfiguration -> {
                    assertThat(toolConfiguration.getDescriptor().getId()).isEqualTo("spotbugs");
                    assertThat(toolConfiguration.getPattern()).isEqualTo("target/spotbugsXml.xml");
                }
        );

//        AggregatedScore score = new AggregatedScore(jsonConfiguration);
//        assertThat(score.getAnalysisConfigurations().getMaxScore()).isEqualTo(111);
    }

    @Test
    void shouldUseDefaultPattern() {
        AnalysisToolsConfiguration configuration = new AnalysisToolsConfiguration();

        ToolConfiguration[] tools = configuration.getTools("{\n"
                + "  \"analysis\": {\n"
                + "    \"tools\": [{\n"
                + "        \"id\": \"spotbugs\"\n"
                + "      }],\n"
                + "    \"maxScore\": 100,\n"
                + "    \"errorImpact\": -5,\n"
                + "    \"highImpact\": -3,\n"
                + "    \"normalImpact\": -2,\n"
                + "    \"lowImpact\": -1\n"
                + "  },\n"
                + "  \"tests\": {\n"
                + "    \"maxScore\": 100,\n"
                + "    \"passedImpact\": 0,\n"
                + "    \"failureImpact\": -5,\n"
                + "    \"skippedImpact\": -1\n"
                + "  },\n"
                + "  \"coverage\": {\n"
                + "    \"maxScore\": 100,\n"
                + "    \"coveredPercentageImpact\": 0,\n"
                + "    \"missedPercentageImpact\": -1\n"
                + "  },\n"
                + "  \"pit\": {\n"
                + "    \"maxScore\": 100,\n"
                + "    \"detectedImpact\": 0,\n"
                + "    \"undetectedImpact\": 0,\n"
                + "    \"detectedPercentageImpact\": 0,\n"
                + "    \"undetectedPercentageImpact\": -1\n"
                + "  }\n"
                + "}\n");

        assertThat(tools).hasSize(1).satisfiesExactly(
                toolConfiguration -> {
                    assertThat(toolConfiguration.getDescriptor().getId()).isEqualTo("spotbugs");
                    assertThat(toolConfiguration.getPattern()).isEqualTo("**/spotbugsXml.xml");
                }
        );
    }

    @Test
    void shouldSkipAnalysisIfNotConfigured() {
        AnalysisToolsConfiguration configuration = new AnalysisToolsConfiguration();

        ToolConfiguration[] tools = configuration.getTools("{\n"
                + "  \"tests\": {\n"
                + "    \"maxScore\": 100,\n"
                + "    \"passedImpact\": 0,\n"
                + "    \"failureImpact\": -5,\n"
                + "    \"skippedImpact\": -1\n"
                + "  },\n"
                + "  \"coverage\": {\n"
                + "    \"maxScore\": 100,\n"
                + "    \"coveredPercentageImpact\": 0,\n"
                + "    \"missedPercentageImpact\": -1\n"
                + "  },\n"
                + "  \"pit\": {\n"
                + "    \"maxScore\": 100,\n"
                + "    \"detectedImpact\": 0,\n"
                + "    \"undetectedImpact\": 0,\n"
                + "    \"detectedPercentageImpact\": 0,\n"
                + "    \"undetectedPercentageImpact\": -1\n"
                + "  }\n"
                + "}\n");

        assertThat(tools).isEmpty();
    }

    @Test
    void shouldFailIfConfigurationOfToolsIsBroken() {
        AnalysisToolsConfiguration configuration = new AnalysisToolsConfiguration();

        assertThatIllegalArgumentException().isThrownBy(
                () -> configuration.getTools("{\n"
                        + "  \"analysis\": {\n"
                        + "    \"tools\": \"spotbugs\",\n"
                        + "    \"maxScore\": 100,\n"
                        + "    \"errorImpact\": -5,\n"
                        + "    \"highImpact\": -3,\n"
                        + "    \"normalImpact\": -2,\n"
                        + "    \"lowImpact\": -1\n"
                        + "  }}\n")
        ).withMessageContaining("'\"spotbugs\"' is not an array");
    }

    @Test
    void shouldFailIfAnalysisConfigurationIsBroken() {
        AnalysisToolsConfiguration configuration = new AnalysisToolsConfiguration();

        assertThatIllegalArgumentException().isThrownBy(
                () -> configuration.getTools("{\n  \"analysis\": \"emtpy\"\n")
        ).withMessageContaining("Wrong configuration for 'analysis' node");
    }

    @Test
    void shouldCreateToolById() {
        ToolConfiguration checkstyle = new ToolConfiguration("checkstyle");
        assertThat(checkstyle.getDescriptor().getName()).isEqualTo("CheckStyle");
        assertThat(checkstyle.getPattern()).isEqualTo("**/checkstyle-result.xml");

        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(
                () -> new ToolConfiguration("not-found")
        ).withMessageContaining("No such parser registered: not-found");
    }
}
