package edu.hm.hafner.grading;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.VisibleForTesting;

/**
 * GitHub action entrypoint for the autograding action.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class AutoGradingAction {
    /**
     * Public entry point, calls the action.
     *
     * @param args
     *         not used
     */
    public static void main(final String... args) {
        new AutoGradingAction().run();
    }

    void run() {
        FilteredLog log = new FilteredLog("Autograding GitHub Action Errors:");
        var logHandler = new LogHandler(System.out, log);

        System.out.println("------------------------------------------------------------------");
        System.out.println("------------------------ Start Grading ---------------------------");
        System.out.println("------------------------------------------------------------------");

        AggregatedScore score = new AggregatedScore(getConfiguration(), log);
        logHandler.print();

        System.out.println("==================================================================");

        GitHubPullRequestWriter pullRequestWriter = new GitHubPullRequestWriter();

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

            GradingReport results = new GradingReport();

            System.out.println(results.getSummary(score));

            pullRequestWriter.addComment(getChecksName(), score,
                    results.getHeader(), results.getSummary(score),
                    results.getDetails(score, List.of()));
        }
        catch (NoSuchElementException | ParsingException | SecureXmlParserFactory.ParsingException exception) {
            System.out.println("==================================================================");
            System.out.println(ExceptionUtils.getStackTrace(exception));

            GradingReport results = new GradingReport();
            pullRequestWriter.addComment(getChecksName(), score,
                    results.getHeader(), results.getSummary(score),
                    results.getErrors(score, exception));

        }

        System.out.println("------------------------------------------------------------------");
        System.out.println("------------------------- End Grading ----------------------------");
        System.out.println("------------------------------------------------------------------");
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
