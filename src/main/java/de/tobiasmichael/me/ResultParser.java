package de.tobiasmichael.me;


import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.parsers.CheckStyleParser;
import se.bjurr.violations.lib.parsers.JCReportParser;
import se.bjurr.violations.lib.parsers.JUnitParser;
import se.bjurr.violations.lib.parsers.PiTestParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

public class ResultParser {

    public static void main(String[] args) {
        JUnitParser jUnitParser = new JUnitParser();

        PiTestParser piTestParser = new PiTestParser();

        CheckStyleParser checkStyleParser = new CheckStyleParser();

        JCReportParser jcReportParser = new JCReportParser();

        try {

            List<Violation> violationList1 = checkStyleParser.parseReportOutput(getReport("target/checkstyle-result.xml"));

            List<Violation> violationList2 = jUnitParser.parseReportOutput(getReport("target/surefire-reports/TEST-de.tobiasmichael.me.MyTest.xml"));

            //List<Violation> violationList3 = jcReportParser.parseReportOutput(getReport("target/jacoco-report/jacoco.xml"));


            printOutput(violationList1);
            printOutput(violationList2);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static String getReport(String filename) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        File fXmlFile = new File(filename);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        StringWriter sw = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(sw));

        return sw.toString();
    }


    private static void printOutput(List<Violation> violations) {
        System.out.println(violations.get(0).getParser());
        String leftAlignFormat = "| %-8s | %-7d | %-80s |%n";
        String leftAlignFormatHeader = "| %-8s | %-7s | %-80s |%n";
        System.out.format("+----------+---------+----------------------------------------------------------------------------------+%n");
        System.out.format(leftAlignFormatHeader, "Severity", "Endline", "Message");
        System.out.format("+----------+---------+----------------------------------------------------------------------------------+%n");
        for (Violation violation: violations) {
            System.out.format(leftAlignFormat, violation.getSeverity(), violation.getEndLine(), violation.getMessage());
        }
        System.out.format("+----------+---------+----------------------------------------------------------------------------------+%n%n");
    }

}
