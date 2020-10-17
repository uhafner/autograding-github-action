package edu.hm.hafner.grading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.parser.FindBugsParser;
import edu.hm.hafner.analysis.parser.FindBugsParser.PriorityProperty;
import edu.hm.hafner.analysis.parser.checkstyle.CheckStyleParser;
import edu.hm.hafner.analysis.parser.pmd.PmdParser;
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
        AggregatedScore score = new AggregatedScore(getConfiguration());

        JacksonFacade jackson = new JacksonFacade();
        System.out.println("Test Configuration: " + jackson.toJson(score.getTestConfiguration()));
        System.out.println("Code Coverage Configuration: " + jackson.toJson(score.getCoverageConfiguration()));
        System.out.println("PIT Mutation Coverage Configuration: " + jackson.toJson(score.getPitConfiguration()));
        AnalysisConfiguration analysisConfiguration = score.getAnalysisConfiguration();
        System.out.println("Static Analysis Configuration: " + jackson.toJson(analysisConfiguration));

        List<Report> testReports = new TestReportFinder().find();
        score.addTestScores(new TestReportSupplier(testReports));

        List<Report> pitReports = new PitReportFinder().find();
        score.addPitScores(new PitReportSupplier(pitReports));

        List<AnalysisScore> analysisReports = new ArrayList<>();
        Report checkStyleReport = new CheckStyleParser().parse(read("target/checkstyle-result.xml"));
        analysisReports.add(createAnalysisScore(analysisConfiguration, "CheckStyle", "checkstyle",
                checkStyleReport));
        Report pmdReport = new PmdParser().parse(read("target/pmd.xml"));
        analysisReports.add(createAnalysisScore(analysisConfiguration, "PMD", "pmd",
                pmdReport));
        Report spotBugsReport = new FindBugsParser(PriorityProperty.RANK).parse(read("target/spotbugsXml.xml"));
        analysisReports.add(createAnalysisScore(analysisConfiguration, "SpotBugs", "spotbugs",
                spotBugsReport));
        score.addAnalysisScores(new AnalysisReportSupplier(analysisReports));

        if (Files.isReadable(Paths.get(JACOCO_RESULTS))) {
            JacocoReport coverageReport = new JacocoParser().parse(read(JACOCO_RESULTS));
            score.addCoverageScores(new CoverageReportSupplier(coverageReport));
        }
        else {
            System.out.println("No JaCoCo coverage result files found!");
        }

        GradingResults results = new GradingResults();

        GitHubPullRequestWriter pullRequestWriter = new GitHubPullRequestWriter();
        pullRequestWriter.addComment(results.getHeader(), results.createSummary(score),
                results.createDetails(score, testReports, Arrays.asList(pmdReport, checkStyleReport, spotBugsReport)),
                testReports, Arrays.asList(pmdReport, checkStyleReport, spotBugsReport));
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
