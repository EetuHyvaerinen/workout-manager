package dev.rezu.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record LogEvent(
        Instant timestamp,
        String threadName,
        LogLevel level,
        String loggerName,
        String message,
        Throwable throwable
) {
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    static LogEvent create(LogLevel level, String loggerName, String message, Throwable throwable) {
        return new LogEvent(
                Instant.now(),
                Thread.currentThread().getName(),
                level,
                loggerName,
                message,
                throwable
        );
    }

    static LogEvent poison() {
        return new LogEvent(Instant.EPOCH, "", LogLevel.DEBUG, "", "", null);
    }

    public void appendTo(StringBuilder sb) {
        sb.append(TIMESTAMP.format(timestamp))
                .append(" [").append(level).append("]")
                .append(" [").append(threadName).append("] ")
                .append(loggerName)
                .append(" - ")
                .append(message)
                .append(System.lineSeparator());

        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }
    }
}