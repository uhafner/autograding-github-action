package de.tobiasmichael.me;

import org.junit.Test;
import static org.junit.Assert.*;

public class MyTest {

    @Test
    public void testConcatenate1() {
        String result = MyMain.concatenate("one", "two");

        assertEquals("onetwo", result);
    }

    @Test
    public void testConcatenate2() {
        String result = MyMain.concatenate("one", "three");

        assertEquals("onethree", result);
    }

    @Test
    public void testConcatenate3() {
        String result = MyMain.concatenate("one", "four");

        assertEquals("onefour", result);
    }

    @Test
    public void testConcatenate4() {
        String result = MyMain.concatenate("one", "five");

        assertEquals("onetwo", result);
    }
}
