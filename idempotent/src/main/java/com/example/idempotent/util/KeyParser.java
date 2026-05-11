package com.example.idempotent.util;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

public class KeyParser {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    public static String parse(String key, Method method, Object[] args) {
        if (key == null || key.isBlank()) {
            return method.toGenericString();
        }

        if (!key.contains("#")) {
            return method.toGenericString() + ":" + key;
        }

        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        if (paramNames == null) {
            return method.toGenericString() + ":" + key;
        }

        EvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < paramNames.length; i++) {
            ctx.setVariable(paramNames[i], args[i]);
        }

        try {
            Object value = PARSER.parseExpression(key).getValue(ctx);
            return method.toGenericString() + ":" + (value == null ? "null" : value.toString());
        } catch (Exception e) {
            return method.toGenericString() + ":" + key;
        }
    }
}
