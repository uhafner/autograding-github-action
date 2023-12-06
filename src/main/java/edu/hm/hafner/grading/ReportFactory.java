package edu.hm.hafner.grading;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import edu.hm.hafner.util.FilteredLog;

/**
 * A base class for all report factories. Provides common functionality to find report files.
 *
 * @author Ullrich Hafner
 */
abstract class ReportFactory {
    protected List<Path> findFiles(final ToolConfiguration tool, final FilteredLog log) {
        log.logInfo("Searching for %s results matching file name pattern %s",
                tool.getDisplayName(), tool.getPattern());
        List<Path> files = new ReportFinder().find("glob:" + tool.getPattern());

        if (files.isEmpty()) {
            throw new NoSuchElementException(String.format("No matching report files found when using pattern '%s'! "
                    + "Configuration error for '%s'?", tool.getPattern(), tool.getDisplayName()));
        }

        Collections.sort(files);
        return files;
    }
}
