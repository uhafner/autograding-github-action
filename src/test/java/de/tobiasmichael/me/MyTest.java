package de.tobiasmichael.me;

import org.junit.Test;
import static org.junit.Assert.*;

public class MyTest {

    @Test
    public void testConcatenate() {
        String result = MyMain.concatenate("one", "two");

        assertEquals("onetwo", result);

    }
}
