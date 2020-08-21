package de.tobiasmichael.me.Util;

import edu.hm.hafner.analysis.ReaderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class JUnitCounter {

    public JUnitCounter() {
    }

    /**
     * Parses JUnit report and returns the number of testcases in it.
     *
     * @param readerFactory readerFactory to get file.
     * @return returns number of testcases in JUnit report.
     */
    public int parse(ReaderFactory readerFactory) {
        Document document = readerFactory.readDocument();
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        NodeList report = root.getElementsByTagName("testcase");
        return report.getLength();
    }

}
