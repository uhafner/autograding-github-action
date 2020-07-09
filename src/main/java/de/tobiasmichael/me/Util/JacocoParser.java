package de.tobiasmichael.me.Util;

import edu.hm.hafner.analysis.ReaderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class JacocoParser {

    public JacocoParser() {
    }

    public JacocoReport parse(ReaderFactory readerFactory) {
        Document document = readerFactory.readDocument();
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        NodeList report = root.getElementsByTagName("counter");

        List<JacocoCounter> jacocoCounterList = new ArrayList<>();
        for (int i = report.getLength() - 1; i > report.getLength() - 6; i--) {
            JacocoCounter jacocoCounter = new JacocoCounter();
            Node node = report.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                jacocoCounter.setType(eElement.getAttribute("type"));
                jacocoCounter.setMissed(Float.parseFloat(eElement.getAttribute("missed")));
                jacocoCounter.setCovered(Float.parseFloat(eElement.getAttribute("covered")));
                jacocoCounterList.add(jacocoCounter);
            }
        }

        JacocoCounter jacocoCounterBranch = getCounterByType(jacocoCounterList, "BRANCH");
        float totalBranchCoveragePercent = 100;
        if (jacocoCounterBranch != null) {
            totalBranchCoveragePercent = jacocoCounterBranch.getCovered() / (jacocoCounterBranch.getCovered() + jacocoCounterBranch.getMissed()) * 100;
        }

        JacocoCounter jacocoCounterLine = getCounterByType(jacocoCounterList, "LINE");
        float totalLineCoveragePercent = 100;
        if (jacocoCounterLine != null) {
            totalLineCoveragePercent = jacocoCounterLine.getCovered() / (jacocoCounterLine.getCovered() + jacocoCounterLine.getMissed()) * 100;
        }
        return new JacocoReport(totalBranchCoveragePercent, totalLineCoveragePercent);
    }

    public JacocoCounter getCounterByType(List<JacocoCounter> jacocoCounterList, String type) {
        for (JacocoCounter jacocoCounter : jacocoCounterList) {
            if (jacocoCounter.getType().equals(type)) {
                return jacocoCounter;
            }
        }
        return null;
    }
}
