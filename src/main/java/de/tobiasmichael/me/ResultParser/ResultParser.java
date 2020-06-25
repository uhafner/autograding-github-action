package de.tobiasmichael.me.ResultParser;


import de.tobiasmichael.me.GithubComment.Commenter;
import edu.hm.hafner.grading.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.parsers.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.List;

public class ResultParser {


    private static String oAuthToken = null;
    private static String gradingConfig = null;


    public static void main(String[] args) {
        if (args.length > 0) {
            gradingConfig = args[0];
            oAuthToken = args[1];

            System.out.println(gradingConfig);
            System.out.println(oAuthToken);
        } else {
            System.out.println("No Token provided, so we'll skip the comment!");
        }

        try {
            Commenter commenter = new Commenter("Test!");
            commenter.commentTo();
        } catch (IOException e) {
            e.printStackTrace();
        }


        JUnitParser jUnitParser = new JUnitParser();

        PiTestParser piTestParser = new PiTestParser();

        CheckStyleParser checkStyleParser = new CheckStyleParser();

        JCReportParser jcReportParser = new JCReportParser();

        PMDParser pmdParser = new PMDParser();

        FindbugsParser findbugsParser = new FindbugsParser();

        try {

            //List<Violation> violationList1 = checkStyleParser.parseReportOutput(getReport("target/checkstyle-result.xml"));

            //List<Violation> violationList2 = jUnitParser.parseReportOutput(getReport("target/surefire-reports/TEST-de.tobiasmichael.me.MyTest.xml"));

            List<Violation> violationList3 = pmdParser.parseReportOutput(getReport("target/pmd.xml"));

            //List<Violation> violationList4 = jcReportParser.parseReportOutput(getReport("target/spotbugs.xml"));

            //writeToFile(getOutput(violationList1), "output1.txt");
            //writeToFile(getOutput(violationList2), "output2.txt");
            writeToFile(getOutput(violationList3), "output3.txt");
            //writeToFile(getOutput(violationList4), "output4.txt");


//            String configuration = "{\"coverage\": {\"maxScore\": 100,\"coveredPercentageImpact\": 0, \"missedPercentageImpact\": -1}";
//            AggregatedScore score = new AggregatedScore(configuration);
//            CoverageSupplier coverageSupplier = new CoverageSupplier() {
//                @Override
//                protected List<CoverageScore> createScores(CoverageConfiguration coverageConfiguration) {
//                    CoverageScore builder =
//                            new CoverageScore.CoverageScoreBuilder().withConfiguration(coverageConfiguration)
//                                    .withId("1")
//                                    .withDisplayName("PDMParser")
//                                    .withCoveredPercentage(100 / violationList3.size())
//                                    .build();
//
//                    return Collections.singletonList(builder);
//                }
//            };
//            score.addCoverageScores(coverageSupplier);
//            System.out.print(score.getCoverageRatio());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static String getReport(String filename) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        try {
            File fXmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            StringWriter sw = new StringWriter();
            t.transform(new DOMSource(doc), new StreamResult(sw));

            return sw.toString();
        } catch (IOException e) {
            throw new NoXMLFileException(filename + " not found!", e);
        }
    }


    private static String getOutput(List<Violation> violations) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("%s%n", violations.get(0).getParser()));
        String leftAlignFormat = "| %-8s | %-7d | %-70s |%n";
        String leftAlignFormatHeader = "| %-8s | %-7s | %-70s |%n";
        stringBuilder.append(String.format("+----------+---------+------------------------------------------------------------------------+%n"));
        stringBuilder.append(String.format(leftAlignFormatHeader, "Severity", "Endline", "Message"));
        stringBuilder.append(String.format("+----------+---------+------------------------------------------------------------------------+%n"));
        for (Violation violation : violations) {
            stringBuilder.append(String.format(leftAlignFormat, violation.getSeverity(), violation.getEndLine(), violation.getMessage()));
        }
        stringBuilder.append(String.format("+----------+---------+------------------------------------------------------------------------+%n%n"));

        return stringBuilder.toString();
    }


    private static void writeToFile(String input, String filename) {
        try {
            FileWriter myWriter = new FileWriter(filename);
            myWriter.write(input);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getoAuthToken() {
        return oAuthToken;
    }
}
