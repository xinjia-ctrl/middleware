package com.example.circuitbreaker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

public class KeyParser {

    private static final Logger log = LoggerFactory.getLogger(KeyParser.class);
    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    public static String parse(String key, Method method, Object[] args) {
        String prefix = method.toGenericString();

        if (key == null || key.isBlank()) {
            return prefix;
        }

        if (!key.contains("#")) {
            return prefix + ":" + key;
        }

        String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
        if (paramNames == null) {
            log.warn("parameter names not available (no -parameters compiler flag?), fallback to raw key: {}", key);
            return prefix + ":" + key;
        }

        EvaluationContext ctx = new StandardEvaluationContext();
        if (args != null) {
            int bound = Math.min(paramNames.length, args.length);
            for (int i = 0; i < bound; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        try {
            Object value = PARSER.parseExpression(key).getValue(ctx);
            return prefix + ":" + (value == null ? "null" : value.toString());
        } catch (Exception e) {
            log.warn("SpEL parse failed for key '{}', fallback to raw key", key, e);
            return prefix + ":" + key;
        }
    }
}
