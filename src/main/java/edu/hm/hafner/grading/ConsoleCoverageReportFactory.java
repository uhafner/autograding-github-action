package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.coverage.registry.ParserRegistry;
import edu.hm.hafner.grading.AggregatedScore.CoverageReportFactory;
import edu.hm.hafner.util.FilteredLog;

/**
 * Reads analysis reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public class ConsoleCoverageReportFactory implements CoverageReportFactory {
    @Override
    public Node create(final ToolConfiguration tool, final FilteredLog log) {
        var parser = new ParserRegistry().getParser(StringUtils.upperCase(tool.getId()), ProcessingMode.FAIL_FAST);

        var name = StringUtils.defaultIfEmpty(tool.getName(), tool.getId());
        log.logInfo("Searching for %s results matching file name pattern %s",
                name, tool.getPattern());
        List<Path> files = new ReportFinder().find("./", "glob:" + tool.getPattern());

        if (files.isEmpty()) {
            log.logError("No matching report files found! Configuration error?");
            return new ModuleNode("empty");
        }

        Collections.sort(files);

        var nodes = new ArrayList<Node>();
        for (Path file : files) {
            var node = parser.parse(new FileReaderFactory(files.get(0)).create(), log);
            log.logInfo("- %s: %s", file, extractMetric(tool, node));
            nodes.add(node);
        }
        var aggregation = Node.merge(nodes);
        log.logInfo("-> %s Total: %s", name, extractMetric(tool, aggregation));
        return aggregation;
    }

    private String extractMetric(final ToolConfiguration tool, final Node node) {

        return node.getValue(Metric.fromTag(tool.getMetric())).map(Value::toString).orElse("<none>");
    }
}
