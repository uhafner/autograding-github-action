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

        float totalInstructionCoveragePercent = getCounterByType(jacocoCounterList, "CLASS").getCovered() / (getCounterByType(jacocoCounterList, "CLASS").getCovered() + getCounterByType(jacocoCounterList, "CLASS").getMissed()) * 100;
        float totalBranchCoveragePercent = getCounterByType(jacocoCounterList, "BRANCH").getCovered() / (getCounterByType(jacocoCounterList, "BRANCH").getCovered() + getCounterByType(jacocoCounterList, "BRANCH").getMissed()) * 100;
        float totalLineCoveragePercent = getCounterByType(jacocoCounterList, "LINE").getCovered() / (getCounterByType(jacocoCounterList, "LINE").getCovered() + getCounterByType(jacocoCounterList, "LINE").getMissed()) * 100;
        float totalComplexityCoveragePercent = getCounterByType(jacocoCounterList, "COMPLEXITY").getCovered() / (getCounterByType(jacocoCounterList, "COMPLEXITY").getCovered() + getCounterByType(jacocoCounterList, "COMPLEXITY").getMissed()) * 100;
        float totalMethodCoveragePercent = getCounterByType(jacocoCounterList, "METHOD").getCovered() / (getCounterByType(jacocoCounterList, "METHOD").getCovered() + getCounterByType(jacocoCounterList, "METHOD").getMissed()) * 100;

        return new JacocoReport(totalInstructionCoveragePercent, totalBranchCoveragePercent, totalLineCoveragePercent, totalComplexityCoveragePercent, totalMethodCoveragePercent);
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
