package edu.hm.hafner.grading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.IssueParser;
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

        System.out.println("Test Configuration: " + jackson.toJson(score.getTestConfiguration()));
        System.out.println("Code Coverage Configuration: " + jackson.toJson(score.getCoverageConfiguration()));
        System.out.println("PIT Mutation Coverage Configuration: " + jackson.toJson(score.getPitConfiguration()));
        AnalysisConfiguration analysisConfiguration = score.getAnalysisConfiguration();
        System.out.println("Static Analysis Configuration: " + jackson.toJson(analysisConfiguration));

        GradingConfiguration configuration = new GradingConfiguration(jsonConfiguration);
        List<Report> testReports = new TestReportFinder().find(configuration.getTestPattern());

        score.addTestScores(new TestReportSupplier(testReports));

        List<Report> pitReports = new PitReportFinder().find();
        score.addPitScores(new PitReportSupplier(pitReports));

        ParserRegistry registry = new ParserRegistry();

        String[] tools = {CHECKSTYLE, PMD, SPOTBUGS};
        List<Report> analysisReports = new ArrayList<>();
        List<AnalysisScore> analysisScores = new ArrayList<>();
        for (String tool : tools) {
            ParserDescriptor parser = registry.get(tool);
            Report report = parse(configuration, parser);
            analysisReports.add(report);
            analysisScores.add(createAnalysisScore(analysisConfiguration, parser.getName(), parser.getId(), report));
        }
        score.addAnalysisScores(new AnalysisReportSupplier(analysisScores));

        if (Files.isReadable(Paths.get(JACOCO_RESULTS))) {
            JacocoReport coverageReport = new JacocoParser().parse(read(JACOCO_RESULTS));
            score.addCoverageScores(new CoverageReportSupplier(coverageReport));
        }
        else {
            System.out.println("No JaCoCo coverage result files found!");
        }

        GradingReport results = new GradingReport();
        GitHubPullRequestWriter pullRequestWriter = new GitHubPullRequestWriter();

        String files = createAffectedFiles(configuration);

        pullRequestWriter.addComment(getChecksName(), results.getHeader(), results.getSummary(score) + files,
                results.getDetails(score, testReports), analysisReports);
    }

    private String createAffectedFiles(final GradingConfiguration configuration) {
        String analysisPattern = configuration.getAnalysisPattern();
        if (StringUtils.isNotBlank(analysisPattern) && !StringUtils.equals(analysisPattern,
                GradingConfiguration.ALL_FILES)) {
            return "\n" + new ReportFinder().renderLinks("./", "regex:" + analysisPattern);
        }
        return StringUtils.EMPTY;
    }

    private Report parse(final GradingConfiguration configuration, final ParserDescriptor descriptor) {
        return filterAnalysisReport(descriptor.createParser().parse(read(descriptor.getPattern())),
                configuration.getAnalysisPattern());
    }

    private Report parse(final GradingConfiguration configuration,
            final IssueParser parser, final String filePattern) {
        return filterAnalysisReport(parser.parse(read(filePattern)), configuration.getAnalysisPattern());
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

    private String getRepository() {
        return StringUtils.defaultIfBlank(System.getenv("GITHUB_REPOSITORY"), "Autograding results");
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

    private static FileReaderFactory read(final String s) {
        return new FileReaderFactory(Paths.get(s));
    }
}
