package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.TestCount;
import edu.hm.hafner.grading.AggregatedScore.TestReportFactory;
import edu.hm.hafner.util.FilteredLog;

/**
 * Reads test reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public final class ConsoleTestReportFactory implements TestReportFactory {
    @Override
    public Node create(final ToolConfiguration tool, final FilteredLog log) {
        var name = StringUtils.defaultIfBlank(tool.getName(), "Tests");

        var delegate = new ConsoleCoverageReportFactory();
        var report = delegate.create(new ToolConfiguration("junit", name, tool.getPattern(), Metric.TESTS.name()), log);

        log.logInfo("-> %s Total: %s tests",
                name, report.getValue(Metric.TESTS).orElse(new TestCount(0)));

        return report;
    }
}
