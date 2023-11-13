package edu.hm.hafner.grading;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;
import edu.hm.hafner.grading.AggregatedScore.TestReportFactory;
import edu.hm.hafner.grading.AggregatedScore.TestResult;
import edu.hm.hafner.util.FilteredLog;

/**
 * Reads test reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public class ConsoleTestReportFactory implements TestReportFactory {
    @Override
    public TestResult create(final ToolConfiguration tool, final FilteredLog log) {
        var name = StringUtils.defaultIfBlank(tool.getName(), "Tests");

        var analysisDelegate = new ConsoleAnalysisReportFactory();
        var report = analysisDelegate.create(new ToolConfiguration("junit", name, tool.getPattern()), log);

        var result = new TestResult(report.getCounter(JUnitAdapter.PASSED_TESTS),
                report.getCounter(JUnitAdapter.FAILED_TESTS), report.getCounter(JUnitAdapter.SKIPPED_TESTS));
        log.logInfo("-> %s Total: %d tests (%d passed, %d failed, %d skipped)",
                name, result.getTotal(), result.getPassedSize(), result.getFailedSize(), result.getSkippedSize());
        return result;
    }
}
