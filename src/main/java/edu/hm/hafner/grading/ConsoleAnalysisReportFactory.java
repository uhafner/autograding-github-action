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
public class ConsoleAnalysisReportFactory extends ReportFactory implements AnalysisReportFactory {
    @Override
    public Report create(final ToolConfiguration tool, final FilteredLog log) {
        ParserDescriptor parser = new ParserRegistry().get(tool.getId());

        var total = new Report(tool.getId(), tool.getDisplayName());

        var analysisParser = parser.createParser();
        for (Path file : findFiles(tool, log)) {
            Report allIssues = analysisParser.parse(new FileReaderFactory(file));
            log.logInfo("- %s: %d warnings", file, allIssues.size());
            total.addAll(allIssues);
        }

        log.logInfo("-> %s Total: %d warnings", tool.getDisplayName(), total.size());
        return total;
    }
}
