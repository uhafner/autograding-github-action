package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.coverage.ContainerNode;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.coverage.registry.ParserRegistry;
import edu.hm.hafner.grading.AggregatedScore.CoverageReportFactory;
import edu.hm.hafner.util.FilteredLog;

/**
 * Reads coverage reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public final class ConsoleCoverageReportFactory implements CoverageReportFactory {
    private static final ReportFinder REPORT_FINDER = new ReportFinder();

    @Override
    public Node create(final ToolConfiguration tool, final FilteredLog log) {
        var parser = new ParserRegistry().getParser(StringUtils.upperCase(tool.getId()), ProcessingMode.FAIL_FAST);

        var nodes = new ArrayList<Node>();
        for (Path file : REPORT_FINDER.find(tool, log)) {
            var node = parser.parse(new FileReaderFactory(file).create(), log);
            log.logInfo("- %s: %s", file, extractMetric(tool, node));
            nodes.add(node);
        }

        if (nodes.isEmpty()) {
            return createContainer(tool);
        }
        else {
            var aggregation = Node.merge(nodes);
            log.logInfo("-> %s Total: %s", tool.getDisplayName(), extractMetric(tool, aggregation));
            if (tool.getName().isBlank()) {
                return aggregation;
            }
            // Wrap the node into a container with the specified tool name
            var containerNode = createContainer(tool);
            containerNode.addChild(aggregation);
            return containerNode;
        }
    }

    private ContainerNode createContainer(final ToolConfiguration tool) {
        return new ContainerNode(tool.getName());
    }

    private String extractMetric(final ToolConfiguration tool, final Node node) {
        return node.getValue(Metric.valueOf(StringUtils.upperCase(tool.getMetric())))
                .map(Value::toString)
                .orElse("<none>");
    }
}
