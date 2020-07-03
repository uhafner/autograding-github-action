package de.tobiasmichael.me.ResultParserTest;

import de.tobiasmichael.me.ResultParser.*;
import org.junit.jupiter.api.Test;

/**
 * Test for ResultParser.
 *
 * @author Tobias Effner
 */
public class ResultParserTest {

    /**
     * This will test the case when there are no JUnit files to parse.
     */
    @Test
    public void testJUniFilesNotPresent() {
        String[] args = {"src/main/java/resources/default.conf"};
        ResultParser.main(args);
    }

    /**
     * This will test the case when one or more JUnit tests not passed.
     */
    @Test
    public void testJUniNotPassed() {
        String[] args = {"src/main/java/resources/default.conf"};
        ResultParser.main(args);
    }

    /**
     * This will test the case when there are many other errors.
     */
    @Test
    public void testJUnitPassedWithMany() {
        String[] args = {"src/main/java/resources/default.conf"};
        ResultParser.main(args);
    }

    /**
     * This will test the case when there are few other errors.
     */
    @Test
    public void testJUnitPassedWithFew() {
        String[] args = {"src/main/java/resources/default.conf"};
        ResultParser.main(args);

    }



}
