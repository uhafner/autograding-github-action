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
        }

        try {
            List<Path> junit_pathList = getPaths("target/surefire-reports/");
            List<Report> junit_reportList = new ArrayList<>();
            junit_pathList.forEach(path -> junit_reportList.add(new JUnitAdapter().parse(new FileReaderFactory(path))));
            int issueCounter = junit_reportList.stream().mapToInt(Report::getSize).sum();


            List<Report> pit_reportList = new ArrayList<>();
            // check if junit generated an issue
            if (issueCounter == 0) {
                logger.info("Going to search for PIT-reports.");
                List<Path> pit_pathList = getPaths("target/pit-reports/");
                pit_pathList.forEach(path -> pit_reportList.add(new PitAdapter().parse(new FileReaderFactory(path))));
            } else {
                throw new NoPITFileException("Not all JUnit tests passed!", junit_reportList);
            }

            Report pmd_report = new PmdParser().parse(new FileReaderFactory(Paths.get("target/pmd.xml")));
            Report checkstyle_report = new CheckStyleParser().parse(new FileReaderFactory(Paths.get("target/checkstyle-result.xml")));
            Report findbugs_report = new FindBugsParser(FindBugsParser.PriorityProperty.RANK).parse(new FileReaderFactory(Paths.get("target/spotbugsXml.xml")));
            JacocoReport jacoco_report = new JacocoParser().parse(new FileReaderFactory(Paths.get("target/site/jacoco/jacoco.xml")));

            String configuration = getGradingConfig();
            AggregatedScore score = new AggregatedScore(configuration);
            score.addAnalysisScores(new AnalysisSupplier() {
                @Override
                protected List<AnalysisScore> createScores(AnalysisConfiguration configuration) {
                    AnalysisScore analysisScore = new AnalysisScore.AnalysisScoreBuilder()
                            .withConfiguration(configuration)
                            .withDisplayName("Analysis")
                            .withId("1")
                            .withTotalErrorsSize(checkstyle_report.getSize())
                            .withTotalHighSeveritySize(checkstyle_report.getSizeOf("high"))
                            .withTotalNormalSeveritySize(checkstyle_report.getSizeOf("normal"))
                            .withTotalLowSeveritySize(checkstyle_report.getSizeOf("low"))
                            .build();
                    return Collections.singletonList(analysisScore);
                }
            });
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
            score.addCoverageScores(new CoverageSupplier() {
                @Override
                protected List<CoverageScore> createScores(CoverageConfiguration configuration) {
                    CoverageScore coverageScore = new CoverageScore.CoverageScoreBuilder()
                            .withConfiguration(configuration)
                            .withDisplayName("Jacoco")
                            .withId("1")
                            .withCoveredPercentage((int) jacoco_report.getInstruction())
                            .build();
                    return Collections.singletonList(coverageScore);
                }
            });
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
                String read = Files.readAllLines(path).get(0);
                logger.info(read);
                input = read;
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
