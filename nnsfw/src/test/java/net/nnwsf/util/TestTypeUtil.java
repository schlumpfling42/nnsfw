package net.nnwsf.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestTypeUtil {

    @BeforeAll
    public static void setupAll() {
        ClassDiscovery.init( "net.nnwsf");
        TypeUtil.init();
    }

    @Test
    public void testInt() {
        int result = (int)TypeUtil.toType("1", int.class);

        assertEquals(1, result);
    }

    @Test
    public void testIntArray() {
        int[] result = (int[])TypeUtil.toType("1,2,3", int[].class);

        assertEquals(3, result.length);
        assertEquals(1, result[0]);
        assertEquals(2, result[1]);
        assertEquals(3, result[2]);
    }

    @Test
    public void testPFloatArray() {
        float[] result = (float[])TypeUtil.toType("1,2,3", float[].class);

        assertEquals(3, result.length);
        assertEquals(1f, result[0]);
        assertEquals(2f, result[1]);
        assertEquals(3f, result[2]);
    }

    @Test
    public void testStringArray() {
        String[] result = (String[])TypeUtil.toType("1,2,3", String[].class);

        assertEquals(3, result.length);
        assertEquals("1", result[0]);
        assertEquals("2", result[1]);
        assertEquals("3", result[2]);
    }
}
