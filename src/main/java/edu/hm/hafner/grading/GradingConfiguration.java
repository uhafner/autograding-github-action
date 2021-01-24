package edu.hm.hafner.grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Grading configuration.
 *
 * @author Ullrich Hafner
 */
public class GradingConfiguration {
    static final String SUREFIRE_REPORT_PATTERN = "glob:./target/surefire-reports/*.xml";
    static final String ALL_FILES = ".*";

    private final String testPattern;
    private final String analysisPattern;

    GradingConfiguration(final String configuration) {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        testPattern = asString(configuration, mapper, "tests", SUREFIRE_REPORT_PATTERN);
        System.out.println("Using test file pattern: " + testPattern);

        analysisPattern = asString(configuration, mapper, "analysis", ALL_FILES);
        System.out.println("Using analysis file pattern: " + analysisPattern);
    }

    private String asString(final String configuration, final ObjectMapper mapper, final String type,
            final String defaultValue) {
        try {
            ObjectNode node = mapper.readValue(configuration, ObjectNode.class);
            JsonNode tests = node.get(type);
            if (tests != null) {
                JsonNode pattern = tests.get("pattern");
                if (pattern != null) {
                    return pattern.asText(defaultValue);
                }
            }
        }
        catch (JsonProcessingException exception) {
            // ignore;
        }
        return defaultValue;
    }

    public String getTestPattern() {
        return testPattern;
    }

    public String getAnalysisPattern() {
        return analysisPattern;
    }
}
