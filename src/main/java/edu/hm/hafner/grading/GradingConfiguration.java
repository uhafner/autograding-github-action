package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

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
    static final String SUREFIRE_DEFAULT_PATTERN = "./target/surefire-reports/*.xml";
    static final String INCLUDE_ALL_FILES = ".*";

    private final String testPattern;
    private final String analysisPattern;
    private final String typesIgnorePattern;

    GradingConfiguration(final String configuration) {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        testPattern = asString(configuration, mapper, "tests", "pattern", SUREFIRE_DEFAULT_PATTERN);
        System.out.println("-> Using test files pattern: " + testPattern);

        analysisPattern = asString(configuration, mapper, "analysis", "fileFilter", INCLUDE_ALL_FILES);
        System.out.println("-> Using file name filter (include) for static analysis: " + analysisPattern);

        typesIgnorePattern = asString(configuration, mapper, "analysis", "typesIgnorePattern", StringUtils.EMPTY);
        if (hasTypeIgnores()) {
            System.out.println("-> Ignoring all warnings types that match the regular expression: " + typesIgnorePattern);
        }
        else {
            System.out.println("-> No warning type ignore pattern specified (property `typesIgnorePattern`)");
        }
    }

    private String asString(final String configuration, final ObjectMapper mapper, final String type,
            final String propertyName, final String defaultValue) {
        try {
            ObjectNode node = mapper.readValue(configuration, ObjectNode.class);
            JsonNode typeNode = node.get(type);
            if (typeNode != null) {
                JsonNode pattern = typeNode.get(propertyName);
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
        return "glob:" + testPattern;
    }

    public String getAnalysisPattern() {
        return analysisPattern;
    }

    public String getTypesIgnorePattern() {
        return typesIgnorePattern;
    }

    public boolean hasTypeIgnores() {
        return !typesIgnorePattern.isEmpty();
    }
}
