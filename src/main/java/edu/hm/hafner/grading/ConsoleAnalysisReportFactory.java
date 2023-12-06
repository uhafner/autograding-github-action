package edu.hm.hafner.grading;

import java.nio.file.Path;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.registry.ParserDescriptor;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.grading.AggregatedScore.AnalysisReportFactory;
import edu.hm.hafner.util.FilteredLog;

/**
 * Reads analysis reports of a specific type from the file system and creates an aggregated report.
 *
 * @author Ullrich Hafner
 */
public final class ConsoleAnalysisReportFactory implements AnalysisReportFactory {
    private static final ReportFinder REPORT_FINDER = new ReportFinder();

    @Override
    public Report create(final ToolConfiguration tool, final FilteredLog log) {
        ParserDescriptor parser = new ParserRegistry().get(tool.getId());

        var total = new Report(tool.getId(), tool.getDisplayName());

        var analysisParser = parser.createParser();
        for (Path file : REPORT_FINDER.find(tool, log)) {
            Report report = analysisParser.parse(new FileReaderFactory(file));
            report.setOrigin(tool.getId(), tool.getDisplayName());
            log.logInfo("- %s: %d warnings", file, report.size());
            total.addAll(report);
        }

        log.logInfo("-> %s Total: %d warnings", tool.getDisplayName(), total.size());
        return total;
    }
}
