package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

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
public class ConsoleAnalysisReportFactory implements AnalysisReportFactory {
    @Override
    public Report create(final ToolConfiguration tool, final FilteredLog log) {
        ParserDescriptor parser = new ParserRegistry().get(tool.getId());

        var displayName = StringUtils.defaultIfBlank(tool.getName(), parser.getName());
        var total = new Report(tool.getId(), displayName);

        log.logInfo("Searching for %s results matching file name pattern %s",
                parser.getName(), tool.getPattern());
        List<Path> files = new ReportFinder().find("./", "glob:" + tool.getPattern());

        if (files.isEmpty()) {
            log.logError("No matching report files found! Configuration error?");
        }
        else {
            Collections.sort(files);

            var analysisParser = parser.createParser();
            for (Path file : files) {
                Report allIssues = analysisParser.parse(new FileReaderFactory(file));
                log.logInfo("- %s: %d warnings", file, allIssues.size());
                total.addAll(allIssues);
            }
        }
        log.logInfo("-> %s Total: %d warnings", displayName, total.size());
        return total;
    }
}
