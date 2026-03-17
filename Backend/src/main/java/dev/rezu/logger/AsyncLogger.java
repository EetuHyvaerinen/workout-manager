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

    public static AsyncLogger getLogger(Class<?> targetClass) {
        return LOGGERS.computeIfAbsent(targetClass.getSimpleName(), AsyncLogger::new);
    }

    public static List<String> getActiveLoggerNames() {
        return List.copyOf(LOGGERS.keySet());
    }

    public static long getDroppedCount() {
        return LogDispatcher.getDroppedCount();
    }
    public static int  getQueueSize() {
        return LogDispatcher.getQueueSize();
    }

    public static int  getQueueCapacity() {
        return LogDispatcher.QUEUE_CAPACITY;
    }

    private final String name;
    private volatile LogLevel minimumLevel = LogLevel.DEBUG;

    private AsyncLogger(String name) {
        this.name = name;
    }

    public void  setLevel(LogLevel level) {
        this.minimumLevel = level;
    }
    public LogLevel getLevel() {
        return minimumLevel;
    }

    public void debug(String msg) {
        log(LogLevel.DEBUG, msg, null);
    }
    public void info(String msg) {
        log(LogLevel.INFO,  msg, null);
    }
    public void warn(String msg) {
        log(LogLevel.WARN,  msg, null);
    }
    public void warn(String msg,  Throwable t) {
        log(LogLevel.WARN,  msg, t);
    }
    public void error(String msg) {
        log(LogLevel.ERROR, msg, null);
    }
    public void error(String msg, Throwable t) {
        log(LogLevel.ERROR, msg, t);
    }

    private void log(LogLevel level, String message, Throwable throwable) {
        if (level.ordinal() < minimumLevel.ordinal()) return;
        LogDispatcher.enqueue(LogEvent.create(level, name, message, throwable));
    }
}