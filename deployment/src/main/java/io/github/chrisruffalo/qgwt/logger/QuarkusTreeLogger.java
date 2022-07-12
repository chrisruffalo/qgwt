package io.github.chrisruffalo.qgwt.logger;

import com.google.common.base.Strings;
import com.google.gwt.core.ext.TreeLogger;
import org.jboss.logging.Logger;

/**
 * Adapts the GWT tree logging system to the Quarkus log.
 */
public class QuarkusTreeLogger extends TreeLogger {

    private static final String INDENT = "  ";
    private final Logger delegate;
    private final int level;

    private Type maxType = Type.INFO;

    public QuarkusTreeLogger(Logger delegate) {
        this(delegate, 0);
    }

    private QuarkusTreeLogger(Logger delegate, int indentLevel) {
        this.delegate = delegate;
        this.level = indentLevel;
    }

    @Override
    public TreeLogger branch(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
        return new QuarkusTreeLogger(delegate, level + 1);
    }

    public void setMaxDetail(Type type) {
        this.delegate.tracef("Max detail for tree logger set to %s", type.name());
        this.maxType = type;
    }

    private Logger.Level levelConverter(final Type type) {
        if(type.isLowerPriorityThan(maxType)) {
            return Logger.Level.TRACE;
        }
        switch (type) {
            case SPAM:
            case TRACE:
            case ALL:
                return Logger.Level.TRACE;
            case DEBUG:
                return Logger.Level.DEBUG;
            case WARN:
                return Logger.Level.WARN;
            case ERROR:
                return Logger.Level.ERROR;
            case INFO:
            default:
                return Logger.Level.INFO;
        }
    }

    @Override
    public boolean isLoggable(Type type) {
        return this.delegate.isEnabled(levelConverter(type));
    }

    @Override
    public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
        this.delegate.log(levelConverter(type), Strings.repeat(INDENT, level) + msg, caught);
    }
}
