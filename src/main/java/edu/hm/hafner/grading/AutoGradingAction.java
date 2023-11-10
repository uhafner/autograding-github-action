package edu.hm.hafner.grading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Report.IssueFilterBuilder;
import edu.hm.hafner.grading.AnalysisScore.AnalysisScoreBuilder;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;
import edu.hm.hafner.util.FilteredLog;

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
        String jsonConfiguration = getConfiguration();

        FilteredLog log = new FilteredLog("Autograding Action Errors:");

        AggregatedScore score = new AggregatedScore(jsonConfiguration, log);

        System.out.println("------------------------------------------------------------------");
        System.out.println("------------------------ Configuration ---------------------------");
        System.out.println("------------------------------------------------------------------");
        System.out.println("Reading configuration: " + jsonConfiguration);

        GradingConfiguration configuration = new GradingConfiguration(jsonConfiguration);

        System.out.println("==================================================================");

        score.gradeTests(new ConsoleTestReportFactory());

        System.out.println("==================================================================");

        score.gradeCoverage(new ConsoleCoverageReportFactory());

        System.out.println("==================================================================");

        score.gradeAnalysis(new ConsoleAnalysisReportFactory());

        System.out.println("==================================================================");

        log.getInfoMessages().forEach(System.out::println);

        System.out.println("==================================================================");

        GradingReport results = new GradingReport();
        GitHubPullRequestWriter pullRequestWriter = new GitHubPullRequestWriter();

        String files = createAffectedFiles(configuration);

        pullRequestWriter.addComment(getChecksName(), results.getHeader(), results.getSummary(score) + files,
                results.getDetails(score, List.of()), List.of());
    }

    private String createAffectedFiles(final GradingConfiguration configuration) {
        String analysisPattern = configuration.getAnalysisPattern();
        if (StringUtils.isNotBlank(analysisPattern) && !StringUtils.equals(analysisPattern,
                GradingConfiguration.INCLUDE_ALL_FILES)) {
            return "\n" + new ReportFinder().renderLinks("./", "regex:" + analysisPattern);
        }
        return StringUtils.EMPTY;
    }

    Report filterAnalysisReport(final Report report, final GradingConfiguration configuration) {
        IssueFilterBuilder builder = new IssueFilterBuilder();
        builder.setIncludeFileNameFilter(configuration.getAnalysisPattern());
        if (configuration.hasTypeIgnores()) {
            builder.setExcludeTypeFilter(configuration.getTypesIgnorePattern());
        }
        return report.filter(builder.build());
    }

    private static AnalysisScore createAnalysisScore(final AnalysisConfiguration configuration,
            final String displayName, final String id, final Report report) {
        return new AnalysisScoreBuilder()
                .withConfiguration(configuration)
                .withName(displayName)
                .withId(id)
                .withReport(report)
                .build();
    }

    private String getChecksName() {
        return StringUtils.defaultIfBlank(System.getenv("CHECKS_NAME"), "Autograding results");
    }

    private String getConfiguration() {
        String configuration = System.getenv("CONFIG");
        if (StringUtils.isBlank(configuration)) {
            System.out.println("No configuration provided (environment CONFIG not set), using default configuration");

            return readDefaultConfiguration();
        }

        System.out.println("Using configuration: " + configuration);
        return configuration;
    }

    private String readDefaultConfiguration() {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get("/default.conf"));

            return new String(encoded, StandardCharsets.UTF_8);
        }
        catch (IOException exception) {
            System.out.println("Can't read configuration: default.conf");
            return StringUtils.EMPTY;
        }
    }
}
