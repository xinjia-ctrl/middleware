package com.example.cacheconsistency.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class KeyParserTest {

    private String sampleMethod(String name, Long id) {
        return "";
    }

    private String noParams() {
        return "";
    }

    private String methodWithNull(String name, Long id) {
        return "";
    }

    @Test
    void shouldParseSpELKey() throws Exception {
        Method method = getClass().getDeclaredMethod("sampleMethod", String.class, Long.class);
        String key = KeyParser.parse("#name + ':' + #id", method, new Object[]{"Alice", 42L});

        assertTrue(key.endsWith(":Alice:42"));
    }

    @Test
    void shouldUseLiteralKeyWhenNoSpEL() throws Exception {
        Method method = getClass().getDeclaredMethod("sampleMethod", String.class, Long.class);
        String key = KeyParser.parse("literal-key", method, new Object[]{"Alice", 42L});

        assertTrue(key.contains("literal-key"));
    }

    @Test
    void shouldFallbackToMethodSignatureWhenKeyIsBlank() throws Exception {
        Method method = getClass().getDeclaredMethod("noParams");
        String key = KeyParser.parse("", method, new Object[]{});

        assertTrue(key.contains("noParams"));
    }

    @Test
    void shouldHandleNullKey() throws Exception {
        Method method = getClass().getDeclaredMethod("noParams");
        String key = KeyParser.parse(null, method, new Object[]{});

        assertTrue(key.contains("noParams"));
    }

    @Test
    void shouldHandleNullArgument() throws Exception {
        Method method = getClass().getDeclaredMethod("methodWithNull", String.class, Long.class);
        String key = KeyParser.parse("#name", method, new Object[]{null, 42L});

        assertTrue(key.endsWith(":null"));
    }

    @Test
    void shouldHandleNestedPropertyExpression() throws Exception {
        Method method = getClass().getDeclaredMethod("sampleMethod", String.class, Long.class);
        String key = KeyParser.parse("#name?.length()", method, new Object[]{"Hello", 42L});

        assertTrue(key.endsWith(":5"));
    }
}
