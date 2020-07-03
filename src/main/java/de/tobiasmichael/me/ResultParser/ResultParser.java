package de.tobiasmichael.me.ResultParser;


import de.tobiasmichael.me.GithubComment.Commenter;
import de.tobiasmichael.me.Util.JacocoParser;
import de.tobiasmichael.me.Util.JacocoReport;
import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.FindBugsParser;
import edu.hm.hafner.analysis.parser.checkstyle.CheckStyleParser;
import edu.hm.hafner.analysis.parser.pmd.PmdParser;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;
import edu.hm.hafner.analysis.parser.violations.PitAdapter;
import edu.hm.hafner.grading.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Result parser
 *
 * @author Tobias Effner
 */
public class ResultParser {

    private static Logger logger;

    private static String oAuthToken = null;
    private static String gradingConfig = null;


    /**
     * Main method; handles logger, arguments, parsers and grading.
     *
     * @param args input arguments
     */
    public static void main(String[] args) {
        logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        logger.setLevel(Level.ALL);

        if (args.length > 0) {
            oAuthToken = args[0];
            if (args.length > 1) {
                gradingConfig = handleGradingConfig(args[1]);
            } else {
                logger.warning("No Config provided, so going to use default config!");
                gradingConfig = handleGradingConfig("src/main/resources/default.conf");
            }
        } else {
            logger.warning("No Token provided, so we'll skip the comment!");
            logger.warning("No Config provided, so going to use default config!");
            gradingConfig = handleGradingConfig("src/main/resources/default.conf");
        }

        try {
            List<Path> junit_pathList = getPaths("target/surefire-reports/");
            if (junit_pathList.size() == 0) logger.warning("No JUnit files found!");
            List<Report> junit_reportList = new ArrayList<>();
            junit_pathList.forEach(path -> junit_reportList.add(new JUnitAdapter().parse(new FileReaderFactory(path))));
            int issueCounter = junit_reportList.stream().mapToInt(Report::getSize).sum();


            List<Report> pit_reportList = new ArrayList<>();
            // check if junit generated an issue
            if (issueCounter == 0) {
                List<Path> pit_pathList = getPaths("target/pit-reports/");
                if (pit_pathList.size() == 0) logger.warning("No PIT files found!");
                pit_pathList.forEach(path -> pit_reportList.add(new PitAdapter().parse(new FileReaderFactory(path))));
            } else {
                if (junit_reportList.size() > 0) {
                    throw new NoPITFileException("Not all JUnit tests passed!", junit_reportList);
                }
            }

            Report pmd_report = null;
            Report checkstyle_report = null;
            Report findbugs_report = null;
            JacocoReport jacoco_report = null;
            try {
                pmd_report = new PmdParser().parse(new FileReaderFactory(Paths.get("target/pmd.xml")));
                checkstyle_report = new CheckStyleParser().parse(new FileReaderFactory(Paths.get("target/checkstyle-result.xml")));
                findbugs_report = new FindBugsParser(FindBugsParser.PriorityProperty.RANK).parse(new FileReaderFactory(Paths.get("target/spotbugsXml.xml")));
                jacoco_report = new JacocoParser().parse(new FileReaderFactory(Paths.get("target/site/jacoco/jacoco.xml")));
            } catch (ParsingException e) {
                logger.severe("One or more XML file(s) not found!");
            }

            String configuration = getGradingConfig();
            AggregatedScore score = new AggregatedScore(configuration);
            if (checkstyle_report != null) {
                Report finalCheckstyle_report = checkstyle_report;
                score.addAnalysisScores(new AnalysisSupplier() {
                    @Override
                    protected List<AnalysisScore> createScores(AnalysisConfiguration configuration) {
                        AnalysisScore analysisScore = new AnalysisScore.AnalysisScoreBuilder()
                                .withConfiguration(configuration)
                                .withDisplayName("Analysis")
                                .withId("1")
                                .withTotalErrorsSize(finalCheckstyle_report.getSize())
                                .withTotalHighSeveritySize(finalCheckstyle_report.getSizeOf("high"))
                                .withTotalNormalSeveritySize(finalCheckstyle_report.getSizeOf("normal"))
                                .withTotalLowSeveritySize(finalCheckstyle_report.getSizeOf("low"))
                                .build();
                        return Collections.singletonList(analysisScore);
                    }
                });
            }
            if (junit_reportList.size() > 0) {
                score.addTestScores(new TestSupplier() {
                    @Override
                    protected List<TestScore> createScores(TestConfiguration configuration) {
                        TestScore testScore = new TestScore.TestScoreBuilder()
                                .withConfiguration(configuration)
                                .withDisplayName("JUnit")
                                .withTotalSize(junit_reportList.get(0).getSize())
                                .withFailedSize(junit_reportList.get(0).getSizeOf("failed"))
                                .withSkippedSize(junit_reportList.get(0).getSizeOf("skipped"))
                                .build();
                        return Collections.singletonList(testScore);
                    }
                });
            }
            if (jacoco_report != null) {
                JacocoReport finalJacoco_report = jacoco_report;
                score.addCoverageScores(new CoverageSupplier() {
                    @Override
                    protected List<CoverageScore> createScores(CoverageConfiguration configuration) {
                        CoverageScore coverageScore = new CoverageScore.CoverageScoreBuilder()
                                .withConfiguration(configuration)
                                .withDisplayName("Jacoco")
                                .withId("1")
                                .withCoveredPercentage((int) finalJacoco_report.getInstruction())
                                .build();
                        return Collections.singletonList(coverageScore);
                    }
                });
            }
            if (pit_reportList.size() > 0) {
                score.addPitScores(new PitSupplier() {
                    @Override
                    protected List<PitScore> createScores(PitConfiguration configuration) {
                        PitScore pitScore = new PitScore.PitScoreBuilder()
                                .withConfiguration(configuration)
                                .withDisplayName("PIT")
                                .withTotalMutations(pit_reportList.get(0).getSizeOf("NORMAL"))
                                .withUndetectedMutations(pit_reportList.get(0).getSizeOf("HIGH"))
                                .build();
                        return Collections.singletonList(pitScore);
                    }
                });
            }

            Commenter commenter = new Commenter(score.toString());
            commenter.commentTo();
        } catch (ParsingException | IOException e) {
            logger.severe(e.toString());
        }
    }

    /**
     * Returns a list of paths that matches the glob pattern.
     *
     * @param location path where to search for files
     * @return list with paths
     * @throws IOException ioexception
     */
    private static List<Path> getPaths(String location) throws IOException {
        String glob = "glob:**/*.xml";
        List<Path> pathList = new ArrayList<>();
        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher(glob);
        Files.walkFileTree(Paths.get(location), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                if (pathMatcher.matches(path)) {
                    pathList.add(path);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return pathList;
    }

    /**
     * Checks if input is a path or a json string and returns valid json;
     * Can lead to an exit if the path was not found or the given string
     * is not a valid json.
     *
     * @param input input string from main method
     * @return valid json string
     */
    private static String handleGradingConfig(String input) {
        Pattern pattern = Pattern.compile(".*/.*");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            try {
                Path path = Paths.get(input);
                input = Files.readAllLines(path).get(0);
                break;
            } catch (IOException e) {
                logger.severe("Config file could not be found!");
                System.exit(1);
            }
        }

        if (isJSONValid(input)) {
            return input;
        } else {
            logger.severe("Config is not a valid JSON!");
            System.exit(1);
        }
        return null;
    }

    /**
     * Checks if the given string is valid json.
     *
     * @param jsonString string to test if it's valid json
     * @return valid json string
     */
    private static boolean isJSONValid(String jsonString) {
        try {
            new JSONObject(jsonString);
        } catch (JSONException ex) {
            try {
                new JSONArray(jsonString);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Getter for oAuthToken.
     *
     * @return oAuthToken
     */
    public static String getOAuthToken() {
        return oAuthToken;
    }

    /**
     * Getter for gradingConfig.
     *
     * @return gradingConfig
     */
    public static String getGradingConfig() {
        return gradingConfig;
    }
}
