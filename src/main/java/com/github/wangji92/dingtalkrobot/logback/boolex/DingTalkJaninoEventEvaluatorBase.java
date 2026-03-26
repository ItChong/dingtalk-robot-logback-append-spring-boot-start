/*
 * Derived from Logback 1.2.x JaninoEventEvaluatorBase (EPL/LGPL dual license, QOS.ch).
 * Logback 1.3+ removed Janino support from core; this class restores Janino-based EvaluatorFilter for this starter.
 */
package com.github.wangji92.dingtalkrobot.logback.boolex;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluatorBase;
import ch.qos.logback.core.boolex.Matcher;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.janino.ScriptEvaluator;

/**
 * Base for Janino-backed event evaluators (replaces removed {@code ch.qos.logback.core.boolex.JaninoEventEvaluatorBase}).
 */
public abstract class DingTalkJaninoEventEvaluatorBase extends EventEvaluatorBase<ILoggingEvent> {

    private static final Class<?> EXPRESSION_TYPE = boolean.class;
    private static final Class<?>[] THROWN_EXCEPTIONS = new Class<?>[1];

    public static final int ERROR_THRESHOLD = 4;

    static {
        THROWN_EXCEPTIONS[0] = EvaluationException.class;
    }

    private String expression;

    private ScriptEvaluator scriptEvaluator;

    private int errorCount;

    protected final List<Matcher> matcherList = new ArrayList<>();

    protected abstract String getDecoratedExpression();

    protected abstract String[] getParameterNames();

    protected abstract Class<?>[] getParameterTypes();

    protected abstract Object[] getParameterValues(ILoggingEvent loggingEvent);

    @Override
    public void start() {
        try {
            if (getContext() == null) {
                throw new IllegalStateException("context must be set before start");
            }
            scriptEvaluator =
                    new ScriptEvaluator(
                            getDecoratedExpression(),
                            EXPRESSION_TYPE,
                            getParameterNames(),
                            getParameterTypes(),
                            THROWN_EXCEPTIONS);
            super.start();
        } catch (Exception e) {
            addError("Could not start evaluator with expression [" + expression + "]", e);
        }
    }

    @Override
    public boolean evaluate(ILoggingEvent event) throws NullPointerException, EvaluationException {
        if (!isStarted()) {
            throw new IllegalStateException("Evaluator [" + getName() + "] was called in stopped state");
        }
        try {
            Boolean result = (Boolean) scriptEvaluator.evaluate(getParameterValues(event));
            return result.booleanValue();
        } catch (Exception ex) {
            errorCount++;
            if (errorCount >= ERROR_THRESHOLD) {
                stop();
            }
            throw new EvaluationException("Evaluator [" + getName() + "] caused an exception", ex);
        }
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public void addMatcher(Matcher matcher) {
        matcherList.add(matcher);
    }

    public List<Matcher> getMatcherList() {
        return matcherList;
    }
}
