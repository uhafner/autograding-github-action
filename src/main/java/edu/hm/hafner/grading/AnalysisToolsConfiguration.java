package edu.hm.hafner.grading;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.hm.hafner.analysis.registry.ParserDescriptor;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Evaluates the tool configuration of the static analysis plugins.
 *
 * @author Ullrich Hafner
 */
public class AnalysisToolsConfiguration {
    ToolConfiguration[] getTools(final String configuration) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ObjectNode node = mapper.readValue(configuration, ObjectNode.class);
            JsonNode typeNode = node.get("analysis");
            if (typeNode != null) {
                JsonNode tools = typeNode.get("tools");
                if (tools != null) {
                    if (tools.isArray()) {
                        return convertToTools(tools);
                    }
                    throw new IllegalArgumentException(
                            String.format("Wrong configuration for 'tools': '%s' is not an array", tools));
                }
            }
        }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Wrong configuration for 'analysis' node: " + configuration);
        }
        return new ToolConfiguration[0];
    }

    private ToolConfiguration[] convertToTools(final JsonNode tools) {
        ToolConfiguration[] configurations = new ToolConfiguration[tools.size()];
        int index = 0;
        for (JsonNode tool : tools) {
            configurations[index++] = new ToolConfiguration(tool.get("id"), tool.get("pattern"));
        }
        return configurations;
    }

    static class ToolConfiguration {
        private static final ParserRegistry TOOLS = new ParserRegistry();

        private final String pattern;
        private final ParserDescriptor descriptor;

        ToolConfiguration(final JsonNode idNode, @CheckForNull final JsonNode patternNode) {
            descriptor = TOOLS.get(idNode.asText());
            if (patternNode == null) {
                pattern = descriptor.getPattern();
            }
            else {
                pattern = patternNode.asText();
            }
        }

        ToolConfiguration(final String id) {
            descriptor = TOOLS.get(id);
            pattern = descriptor.getPattern();
        }

        ParserDescriptor getDescriptor() {
            return descriptor;
        }

        String getPattern() {
            return pattern;
        }

        @Override
        public String toString() {
            return String.format("%s with pattern '%s'", descriptor.getName(), pattern);
        }
    }
}
