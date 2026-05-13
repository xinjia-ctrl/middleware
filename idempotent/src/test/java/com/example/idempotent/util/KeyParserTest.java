package com.example.idempotent.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class KeyParserTest {

    private String sampleMethod(String arg1, Integer arg2) {
        return arg1 + arg2;
    }

    @Test
    void shouldReturnMethodSignatureWhenKeyIsEmpty() throws Exception {
        Method method = getClass().getDeclaredMethod("sampleMethod", String.class, Integer.class);
        String key = KeyParser.parse("", method, new Object[]{"hello", 42});
        assertFalse(key.contains("null"));
        assertTrue(key.contains("sampleMethod"));
    }

    @Test
    void shouldParseSpelExpression() throws Exception {
        Method method = getClass().getDeclaredMethod("sampleMethod", String.class, Integer.class);
        String key = KeyParser.parse("#arg1", method, new Object[]{"hello", 42});
        assertTrue(key.endsWith(":hello"));
    }

    @Test
    void shouldHandleComplexExpression() throws Exception {
        Method method = getClass().getDeclaredMethod("sampleMethod", String.class, Integer.class);
        String key = KeyParser.parse("#arg1 + ':' + #arg2", method, new Object[]{"order", 123});
        assertTrue(key.endsWith(":order:123"));
    }
}
