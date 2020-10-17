package edu.hm.hafner.grading;

import org.apache.commons.io.FilenameUtils;

import edu.hm.hafner.analysis.Report;

/**
 * Renders the file name for a {@link Report}.
 *
 * @author Ullrich Hafner
 */
class FileNameRenderer {
    String getFileName(final Report report, final String defaultFileName) {
        return FilenameUtils.getBaseName(report.getFileNames().stream().findAny().orElse(defaultFileName));
    }
}
