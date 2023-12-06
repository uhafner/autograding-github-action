package edu.hm.hafner.grading;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter.ChecksStatus;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.VisibleForTesting;

/**
 * GitHub action entrypoint for the autograding action.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "PMD.SystemPrintln"})
public class AutoGradingAction {
    /**
     * Public entry point, calls the action.
     *
     * @param unused
     *         not used
     */
    public static void main(final String... unused) {
        new AutoGradingAction().run();
    }

    void run() {
        var log = new FilteredLog("Autograding GitHub Action Errors:");
        var logHandler = new LogHandler(System.out, log);

        System.out.println("------------------------------------------------------------------");
        System.out.println("------------------------ Start Grading ---------------------------");
        System.out.println("------------------------------------------------------------------");

        var score = new AggregatedScore(getConfiguration(), log);
        logHandler.print();

        System.out.println("==================================================================");

        var pullRequestWriter = new GitHubPullRequestWriter();

        try {
            score.gradeTests(new ConsoleTestReportFactory());
            logHandler.print();

            System.out.println("==================================================================");

            score.gradeCoverage(new ConsoleCoverageReportFactory());
            logHandler.print();

            System.out.println("==================================================================");

            score.gradeAnalysis(new ConsoleAnalysisReportFactory());
            logHandler.print();

            System.out.println("==================================================================");

            var results = new GradingReport();

            System.out.println(results.getTextSummary(score));

            System.out.println("==================================================================");

            System.out.println("Commenting on commit or pull request...");

            var errors = createErrorMessage(log);

            pullRequestWriter.addComment(getChecksName(), score,
                    results.getHeader(), results.getTextSummary(score),
                    results.getMarkdownDetails(score) + errors,
                    results.getMarkdownSummary(score, ":mortar_board: " + getChecksName()) + errors,
                    ChecksStatus.SUCCESS);

            var environmentVariables = createEnvironmentVariables(score);
            Files.writeString(Paths.get("metrics.env"), environmentVariables, StandardOpenOption.CREATE);
        }
        catch (NoSuchElementException
               | IOException
               | ParsingException
               | SecureXmlParserFactory.ParsingException exception) {
            System.out.println("==================================================================");
            System.out.println(ExceptionUtils.getStackTrace(exception));

            var results = new GradingReport();
            pullRequestWriter.addComment(getChecksName(), score,
                    results.getHeader(), results.getTextSummary(score),
                    results.getMarkdownErrors(score, exception),
                    results.getMarkdownErrors(score, exception),
                    ChecksStatus.ERROR);

        }

        System.out.println("------------------------------------------------------------------");
        System.out.println("------------------------- End Grading ----------------------------");
        System.out.println("------------------------------------------------------------------");
    }

    private String createErrorMessage(final FilteredLog log) {
        if (log.hasErrors()) {
            StringBuilder errors = new StringBuilder();

            errors.append("## :construction: Error Messages\n```\n");
            var messages = new StringJoiner("\n");
            log.getErrorMessages().forEach(messages::add);
            errors.append(messages);
            errors.append("\n```\n");

            return errors.toString();
        }
        return StringUtils.EMPTY;
    }

    String createEnvironmentVariables(final AggregatedScore score) {
        var metrics = new StringBuilder();
        score.getMetrics().forEach((metric, value) -> metrics.append(String.format("%s=%d%n", metric, value)));
        System.out.println("------------------------------------------------------------------");
        System.out.println("--------------------------- Summary ------------------------------");
        System.out.println("------------------------------------------------------------------");
        System.out.println(metrics);
        return metrics.toString();
    }

    private String getChecksName() {
        return StringUtils.defaultIfBlank(System.getenv("CHECKS_NAME"), "Autograding results");
    }

    @VisibleForTesting
    String getConfiguration() {
        String configuration = System.getenv("CONFIG");
        if (StringUtils.isBlank(configuration)) {
            System.out.println("No configuration provided (environment variable CONFIG not set), using default configuration");

            return readDefaultConfiguration();
        }

        System.out.println("Obtaining configuration from environment variable CONFIG");
        return configuration;
    }

    private String readDefaultConfiguration() {
        try {
            var defaultConfig = getClass().getResource("/default-config.json");
            if (defaultConfig == null) {
                throw new IOException("Can't find configuration in class path: default-conf.json");
            }
            return Files.readString(Paths.get(defaultConfig.toURI()));
        }
        catch (IOException | URISyntaxException exception) {
            System.out.println("Can't read configuration: default.conf");
            return StringUtils.EMPTY;
        }
    }
}
