package edu.hm.hafner.grading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Report.IssueFilterBuilder;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.registry.ParserDescriptor;
import edu.hm.hafner.analysis.registry.ParserRegistry;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;

import de.tobiasmichael.me.Util.JacocoParser;
import de.tobiasmichael.me.Util.JacocoReport;

/**
 * GitHub action entrypoint for the autograding action.
 *
 * @author Tobias Effner
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class AutoGradingAction {
    private static final String JACOCO_RESULTS = "target/site/jacoco/jacoco.xml";
    private static final String CHECKSTYLE = "checkstyle";
    private static final String PMD = "pmd";
    private static final String SPOTBUGS = "spotbugs";

    /**
     * Public entry point, calls the action.
     *
     * @param args
     *         not used
     */
    public static void main(final String[] args) {
        new AutoGradingAction().run();
    }

    void run() {
        String jsonConfiguration = getConfiguration();
        AggregatedScore score = new AggregatedScore(jsonConfiguration);

        JacksonFacade jackson = new JacksonFacade();

        System.out.println("------------------------------------------------------------------");
        System.out.println("------------------------ Configuration ---------------------------");
        System.out.println("------------------------------------------------------------------");
        System.out.println("-> Test Configuration: " + jackson.toJson(score.getTestConfiguration()));
        System.out.println("-> Code Coverage Configuration: " + jackson.toJson(score.getCoverageConfiguration()));
        System.out.println("-> PIT Mutation Coverage Configuration: " + jackson.toJson(score.getPitConfiguration()));
        System.out.println("-> Static Analysis Configuration: " + jackson.toJson(score.getAnalysisConfiguration()));

        GradingConfiguration configuration = new GradingConfiguration(jsonConfiguration);

        System.out.println("==================================================================");
        List<Report> testReports = new TestReportFinder().find(configuration.getTestPattern());
        score.addTestScores(new TestReportSupplier(testReports));
        System.out.println("==================================================================");
        List<Report> pitReports = new PitReportFinder().find();
        score.addPitScores(new PitReportSupplier(pitReports));
        System.out.println("==================================================================");
        if (Files.isReadable(Paths.get(JACOCO_RESULTS))) {
            JacocoReport coverageReport = new JacocoParser().parse(new FileReaderFactory(Paths.get(JACOCO_RESULTS)));
            score.addCoverageScores(new CoverageReportSupplier(coverageReport));
        }
        else {
            System.out.println("No JaCoCo coverage result files found!");
        }
        System.out.println("==================================================================");
        ReportFinder reportFinder = new ReportFinder();
        ParserRegistry registry = new ParserRegistry();

        String[] tools = {CHECKSTYLE, PMD, SPOTBUGS};
        List<Report> analysisReports = new ArrayList<>();
        List<AnalysisScore> analysisScores = new ArrayList<>();

        for (String tool : tools) {
            ParserDescriptor parser = registry.get(tool);
            List<Path> files = reportFinder.find("target", "glob:" + parser.getPattern());
            System.out.format("Searching for '%s' results matching file name pattern %s%n", parser.getName(), parser.getPattern());

            if (files.size() == 0) {
                System.out.println("No matching report result files found!");
            }
            else {
                Collections.sort(files);

                for (Path file : files) {
                    Report allIssues = parser.createParser().parse(new FileReaderFactory(file));
                    Report filteredIssues = filterAnalysisReport(allIssues, configuration.getAnalysisPattern());
                    System.out.format("- %s : %d warnings (from total %d)%n", file, filteredIssues.size(), allIssues.size());
                    analysisReports.add(filteredIssues);
                    analysisScores.add(createAnalysisScore(score.getAnalysisConfiguration(), parser.getName(),
                            parser.getId(), filteredIssues));
                }
            }
        }
        score.addAnalysisScores(new AnalysisReportSupplier(analysisScores));
        System.out.println("==================================================================");

        GradingReport results = new GradingReport();
        GitHubPullRequestWriter pullRequestWriter = new GitHubPullRequestWriter();

        String files = createAffectedFiles(configuration);

        pullRequestWriter.addComment(getChecksName(), results.getHeader(), results.getSummary(score) + files,
                results.getDetails(score, testReports), analysisReports);
    }

    private String createAffectedFiles(final GradingConfiguration configuration) {
        String analysisPattern = configuration.getAnalysisPattern();
        if (StringUtils.isNotBlank(analysisPattern) && !StringUtils.equals(analysisPattern,
                GradingConfiguration.INCLUDE_ALL_FILES)) {
            return "\n" + new ReportFinder().renderLinks("./", "regex:" + analysisPattern);
        }
        return StringUtils.EMPTY;
    }

    private Report filterAnalysisReport(final Report checkStyleReport, final String analysisPattern) {
        IssueFilterBuilder builder = new IssueFilterBuilder();
        builder.setIncludeFileNameFilter(analysisPattern);
        return checkStyleReport.filter(builder.build());
    }

    private static AnalysisScore createAnalysisScore(final AnalysisConfiguration configuration,
            final String displayName,
            final String id, final Report report) {
        return new AnalysisScore.AnalysisScoreBuilder()
                .withConfiguration(configuration)
                .withDisplayName(displayName)
                .withId(id)
                .withTotalErrorsSize(report.getSizeOf(Severity.ERROR))
                .withTotalHighSeveritySize(report.getSizeOf(Severity.WARNING_HIGH))
                .withTotalNormalSeveritySize(report.getSizeOf(Severity.WARNING_NORMAL))
                .withTotalLowSeveritySize(report.getSizeOf(Severity.WARNING_LOW))
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
