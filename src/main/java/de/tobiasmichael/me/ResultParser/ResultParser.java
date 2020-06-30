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
            List<String> stringList = new ArrayList<>();


            List<Path> junit_pathList = getPaths("target/surefire-reports/");
            List<Report> junit_reportList = new ArrayList<>();
            junit_pathList.forEach(path1 -> junit_reportList.add(new JUnitAdapter().parse(new FileReaderFactory(path1))));
            int issue_counter = junit_reportList.stream().mapToInt(Report::getSize).sum();
            junit_reportList.forEach(junit_report1 -> stringList.add("\nJUnit // " + junit_report1.toString()));


            if (issue_counter == 0) {
                List<Path> pit_pathList = getPaths("target/pit-reports/");
                List<Report> pit_reportList = new ArrayList<>();
                pit_pathList.forEach(path1 -> pit_reportList.add(new PitAdapter().parse(new FileReaderFactory(path1))));
                pit_reportList.forEach(pit_report1 -> stringList.add("\nPIT // " + pit_report1.toString()));

            }

            Report pmd_report = new PmdParser().parse(new FileReaderFactory(Paths.get("target/pmd.xml")));
            stringList.add("\nPMD // " + pmd_report.toString());

            Report checkstyle_report = new CheckStyleParser().parse(new FileReaderFactory(Paths.get("target/checkstyle-result.xml")));
            stringList.add("\nCheckStyle // " + checkstyle_report.toString());

            Report findbugs_report = new FindBugsParser(FindBugsParser.PriorityProperty.RANK).parse(new FileReaderFactory(Paths.get("target/spotbugsXml.xml")));
            stringList.add("\nFindBugs // " + findbugs_report.toString());

            Report jacoco_report = new CodeAnalysisParser().parse(new FileReaderFactory(Paths.get("target/site/jacoco/jacoco.xml")));
            stringList.add("\nJacoco // " + jacoco_report.toString());

            commentTo(stringList);


            String configuration = "{\"analysis\": { \"maxScore\": 100, \"errorImpact\": -5}}";
            AggregatedScore score = new AggregatedScore(configuration);
            score.addAnalysisScores(new AnalysisSupplier() {
                @Override
                protected List<AnalysisScore> createScores(AnalysisConfiguration configuration) {
                    
                    return null;
                }
            });
            score.addTestScores(new TestSupplier() {
                @Override
                protected List<TestScore> createScores(TestConfiguration configuration) {
                    return null;
                }
            });
            score.addCoverageScores(new CoverageSupplier() {
                @Override
                protected List<CoverageScore> createScores(CoverageConfiguration configuration) {
                    return null;
                }
            });

            score.getAnalysisScores().forEach(System.out::println);

        } catch (ParsingException | IOException e) {
            try {
                throw new NoXMLFileException("File not found!", e);
            } catch (NoXMLFileException noXMLFileException) {
                noXMLFileException.printStackTrace();
            }
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

    public static String getoAuthToken() {
        return oAuthToken;
    }
}
