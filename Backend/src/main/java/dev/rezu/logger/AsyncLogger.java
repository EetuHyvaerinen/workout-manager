package dev.rezu.logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AsyncLogger {

    private static final Map<String, AsyncLogger> LOGGERS = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(
                Thread.ofPlatform().name("async-logger-shutdown").unstarted(LogDispatcher::shutdown)
        );
    }

    public static AsyncLogger getLogger(Class<?> clazz) {
        return LOGGERS.computeIfAbsent(clazz.getSimpleName(), AsyncLogger::new);
    }

    public static List<String> getActiveLoggerNames() {
        return List.copyOf(LOGGERS.keySet());
    }

    public static long getDroppedCount() { return LogDispatcher.getDroppedCount(); }
    public static int  getQueueSize()    { return LogDispatcher.getQueueSize();    }
    public static int  getQueueCapacity(){ return LogDispatcher.QUEUE_CAPACITY;    }

    public enum Level { DEBUG, INFO, WARN, ERROR }

    private final String name;
    private volatile Level minimumLevel = Level.DEBUG;

    private AsyncLogger(String name) {
        this.name = name;
    }

    public void  setLevel(Level level) { this.minimumLevel = level; }
    public Level getLevel()            { return minimumLevel; }

    public void debug(String msg)              { log(Level.DEBUG, msg, null); }
    public void info(String msg)               { log(Level.INFO,  msg, null); }
    public void warn(String msg)               { log(Level.WARN,  msg, null); }
    public void warn(String msg,  Throwable t) { log(Level.WARN,  msg, t);    }
    public void error(String msg)              { log(Level.ERROR, msg, null); }
    public void error(String msg, Throwable t) { log(Level.ERROR, msg, t);    }

    private void log(Level level, String message, Throwable throwable) {
        if (level.ordinal() < minimumLevel.ordinal()) return;
        LogDispatcher.enqueue(LogEvent.create(level, name, message, throwable));
    }
}