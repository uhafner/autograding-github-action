package edu.hm.hafner.grading;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link LogHandler}.
 *
 * @author Andreas Neumeier
 */
class LogHandlerTest {
    private static final String LOG_HANDLER_NAME = "TestHandler";
    private static final String MESSAGE = "TestMessage";
    private static final String NOT_SHOWN = "Not shown";
    private static final String ADDITIONAL_MESSAGE = "Additional";
    private static final String LOGGER_MESSAGE = "Logger message";

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "Log some messages and evaluate quiet flag value (quiet = {0})")
    void shouldLogInfoAndErrorMessage(final boolean quiet) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream);

        FilteredLog logger = new FilteredLog("Title");
        logger.logInfo(NOT_SHOWN);
        logger.logError(NOT_SHOWN);

        LogHandler logHandler = new LogHandler(printStream, logger);
        logHandler.setQuiet(quiet);

        logger.logInfo("Info 1");
        logger.logInfo("Info 2");
        logger.logError("Error 1");

        logHandler.print();

        if (quiet) {
            assertThat(outputStream.toString()).isEmpty();
        }
        else {
            assertThat(outputStream.toString()).contains("Info 1", "Info 2", "Error 1");
        }

        logger.logInfo("Info 3");
        logger.logInfo("Info 4");
        logger.logError("Error 2");

        logHandler.print();

        if (quiet) {
            assertThat(outputStream.toString()).isEmpty();
        }
        else {
            assertThat(outputStream.toString()).contains("Info 1", "Info 2", "Error 1", "Info 3", "Info 4", "Error 2");
        }
    }
}
