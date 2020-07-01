package de.tobiasmichael.me.ResultParser;


import de.tobiasmichael.me.GithubComment.Commenter;
import edu.hm.hafner.analysis.FileReaderFactory;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.parser.CodeAnalysisParser;
import edu.hm.hafner.analysis.parser.FindBugsParser;
import edu.hm.hafner.analysis.parser.checkstyle.CheckStyleParser;
import edu.hm.hafner.analysis.parser.pmd.PmdParser;
import edu.hm.hafner.analysis.parser.violations.JUnitAdapter;
import edu.hm.hafner.analysis.parser.violations.PitAdapter;
import edu.hm.hafner.grading.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultParser {


    private static String oAuthToken = null;
    private static String gradingConfig = null;


    public static void main(String[] args) {
        if (args.length > 0) {
            gradingConfig = args[0];
            oAuthToken = args[1];
        } else {
            System.out.println("No Token provided, so we'll skip the comment!");
        }

        try {
            List<Path> junit_pathList = getPaths("target/surefire-reports/");
            List<Report> junit_reportList = new ArrayList<>();
            junit_pathList.forEach(path -> junit_reportList.add(new JUnitAdapter().parse(new FileReaderFactory(path))));
            int issueCounter = junit_reportList.stream().mapToInt(Report::getSize).sum();


            List<Report> pit_reportList = new ArrayList<>();
            // check if junit generated an issue
            if (issueCounter == 0) {
                List<Path> pit_pathList = getPaths("target/pit-reports/");
                pit_pathList.forEach(path1 -> pit_reportList.add(new PitAdapter().parse(new FileReaderFactory(path1))));
            } else {
                throw new NoPITFileException("Not all JUnit tests passed!", junit_reportList);
            }

            Report pmd_report = new PmdParser().parse(new FileReaderFactory(Paths.get("target/pmd.xml")));
            Report checkstyle_report = new CheckStyleParser().parse(new FileReaderFactory(Paths.get("target/checkstyle-result.xml")));
            Report findbugs_report = new FindBugsParser(FindBugsParser.PriorityProperty.RANK).parse(new FileReaderFactory(Paths.get("target/spotbugsXml.xml")));
            Report jacoco_report = new CodeAnalysisParser().parse(new FileReaderFactory(Paths.get("target/site/jacoco/jacoco.xml")));

            String configuration = "{\"analysis\": { \"maxScore\": 100, \"errorImpact\": -5}}";
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
//            score.addCoverageScores(new CoverageSupplier() {
//                @Override
//                protected List<CoverageScore> createScores(CoverageConfiguration configuration) {
//                    return null;
//                }
//            });
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
            e.printStackTrace();
        }
    }

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

    private static void commentTo(List<String> strings) {
        StringBuilder stringBuilder = new StringBuilder();
        strings.forEach(stringBuilder::append);

        Commenter commenter = new Commenter(stringBuilder.toString());
        commenter.commentTo();
    }

    public static String getOAuthToken() {
        return oAuthToken;
    }
}
