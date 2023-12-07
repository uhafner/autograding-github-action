package edu.hm.hafner.grading;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.grading.github.GitHubPullRequestWriter;
import edu.hm.hafner.util.FilteredLog;

import static org.assertj.core.api.Assertions.*;

class ConsoleAnalysisReportFactoryTest {
    private static final String CONFIGURATION = """
            {
              "analysis": [
                {
                  "name": "Style",
                  "id": "style",
                  "tools": [
                    {
                      "id": "checkstyle",
                      "name": "CheckStyle",
                      "pattern": "**/src/**/checkstyle*.xml"
                    },
                    {
                      "id": "pmd",
                      "name": "PMD",
                      "pattern": "**/src/**/pmd*.xml"
                    }
                  ],
                  "errorImpact": 1,
                  "highImpact": 2,
                  "normalImpact": 3,
                  "lowImpact": 4,
                  "maxScore": 100
                },
                {
                  "name": "Bugs",
                  "id": "bugs",
                  "tools": [
                    {
                      "id": "spotbugs",
                      "name": "SpotBugs",
                      "pattern": "**/src/**/spotbugs*.xml"
                    }
                  ],
                  "errorImpact": -11,
                  "highImpact": -12,
                  "normalImpact": -13,
                  "lowImpact": -14,
                  "maxScore": 100
                }
              ]
            }
            """;
    private static final int EXPECTED_ISSUES = 19 + 41 + 1;

    @Test
    void shouldCreateAggregation() {
        var log = new FilteredLog("Errors");
        var score = new AggregatedScore(CONFIGURATION, log);

        score.gradeAnalysis(new ConsoleAnalysisReportFactory());

        assertThat(score.getIssues()).hasSize(EXPECTED_ISSUES);
        assertThat(score.getIssues()).extracting(Issue::getBaseName).containsOnly(
                "LogHandler.java",
                "Assignment00.java",
                "Assignment01.java",
                "Assignment02.java",
                "Assignment03.java",
                "Assignment04.java",
                "Assignment05.java",
                "Assignment06.java",
                "Assignment07.java",
                "Assignment08.java",
                "Assignment09.java",
                "Assignment10.java",
                "Assignment11.java",
                "Assignment12.java",
                "Assignment13.java",
                "Assignment13.java",
                "Assignment14.java",
                "Assignment14.java",
                "Assignment14.java",
                "Assignment15.java",
                "Assignment15.java",
                "Assignment15.java",
                "Assignment16.java",
                "AbstractKaraTest.java",
                "Assignment00Test.java",
                "Assignment01Test.java",
                "Assignment02Test.java",
                "Assignment03Test.java",
                "Assignment04Test.java",
                "Assignment05Test.java",
                "Assignment06Test.java",
                "Assignment07Test.java",
                "Assignment08Test.java",
                "Assignment09Test.java",
                "Assignment10Test.java",
                "Assignment11Test.java",
                "Assignment12Test.java",
                "Assignment13Test.java",
                "Assignment14Test.java",
                "Assignment15Test.java",
                "Assignment16Test.java",
                "ReportFactory.java",
                "AutoGradingAction.java");
        assertThat(log.getInfoMessages()).contains(
                "Searching for CheckStyle results matching file name pattern **/src/**/checkstyle*.xml",
                "- ./src/test/resources/checkstyle/checkstyle-ignores.xml: 18 warnings",
                "- ./src/test/resources/checkstyle/checkstyle-result.xml: 1 warnings",
                "-> CheckStyle Total: 19 warnings",
                "Searching for PMD results matching file name pattern **/src/**/pmd*.xml",
                "- ./src/test/resources/pmd/pmd-ignores.xml: 40 warnings",
                "- ./src/test/resources/pmd/pmd.xml: 1 warnings",
                "-> PMD Total: 41 warnings",
                "=> Style Score: 100 of 100",
                "Searching for SpotBugs results matching file name pattern **/src/**/spotbugs*.xml",
                "- ./src/test/resources/spotbugs/spotbugsXml.xml: 1 warnings",
                "-> SpotBugs Total: 1 warnings",
                "=> Bugs Score: 86 of 100");

        var writer = new GitHubPullRequestWriter();
        assertThat(writer.createAnnotations(score)).hasSize(EXPECTED_ISSUES).extracting("message")
                .containsOnly("Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Hilfsklassen sollten keinen Standard-Konstruktur und keinen als public deklarierten Konstruktor haben.",
                        "Vor ',' befindet sich ein Leerzeichen.",
                        "Es fehlt ein Javadoc-Kommentar.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "Consider using varargs for methods or constructors which take an array the last parameter.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "The method 'main(String...)' has a cognitive complexity of 16, current threshold is 15.",
                        "These nested if statements could be combined.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "Avoid using Literals in Conditional Statements.",
                        "Consider using varargs for methods or constructors which take an array the last parameter.",
                        "All methods are static.  Consider using a utility class instead. Alternatively, you could add a private constructor or make the class abstract to silence this warning.",
                        "This abstract class does not have any abstract methods.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "Linguistics Antipattern - The method 'shouldSolveAssignment' indicates linguistically it returns a boolean, but it returns 'Stream'.",
                        "This abstract class does not have any abstract methods.",
                        "Usage of GetResource in edu.hm.hafner.grading.AutoGradingAction.readDefaultConfiguration() may be\n      unsafe if class is extended");
    }
}
