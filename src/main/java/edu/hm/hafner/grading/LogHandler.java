package edu.hm.hafner.grading;

import java.io.PrintStream;

import edu.hm.hafner.util.FilteredLog;

/**
 * Handles logging of log and error messages to a {@link PrintStream} instance.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.LoggerIsNotStaticFinal")
public class LogHandler {
    private final PrintStream printStream;
    private final FilteredLog logger;

    private int infoPosition;
    private int errorPosition;
    private boolean quiet = false;

    /**
     * Creates a new {@link LogHandler}.
     *
     * @param printStream
     *         the task listener that will print all log messages
     * @param logger
     *         the logger that contains the actual log messages
     */
    public LogHandler(final PrintStream printStream, final FilteredLog logger) {
        this.printStream = printStream;
        this.logger = logger;

        this.infoPosition = getSizeOfInfoMessages();
        this.errorPosition = logger.getErrorMessages().size();
    }

    private int getSizeOfInfoMessages() {
        return logger.getInfoMessages().size();
    }

    private int getSizeOfErrorMessages() {
        return logger.getErrorMessages().size();
    }

    /**
     * Prints all new log messages to the {@link PrintStream}.
     */
    public void print() {
        printInfoMessages();
        printErrorMessages();
    }

    private void printInfoMessages() {
        var size = getSizeOfInfoMessages();
        if (infoPosition < size && !quiet) {
            logger.getInfoMessages().subList(infoPosition, size).forEach(printStream::println);
            infoPosition = logger.getInfoMessages().size();
        }
    }

    private void printErrorMessages() {
        var size = getSizeOfErrorMessages();
        if (errorPosition < size && !quiet) {
            logger.getErrorMessages().subList(errorPosition, size).forEach(printStream::println);
            errorPosition = logger.getErrorMessages().size();
        }
    }

    public void setQuiet(final boolean quiet) {
        this.quiet = quiet;
    }
}
