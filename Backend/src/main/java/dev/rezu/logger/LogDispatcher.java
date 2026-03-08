package dev.rezu.logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

final class LogDispatcher {

    static final int  QUEUE_CAPACITY     = 50_000;
    private static final int  BATCH_SIZE         = 512;
    private static final long FLUSH_INTERVAL_MS  = 200;
    private static final int  BUFFER_SIZE        = 8 * 1024;
    private static final String LOG_DIR          = "logs";
    private static final long MAX_FILE_SIZE      = 10 * 1024 * 1024;
    private static final int  MAX_BACKUPS        = 10;

    private static final LogEvent POISON = LogEvent.poison();

    private static final BlockingQueue<LogEvent> QUEUE   = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private static final AtomicLong              DROPPED = new AtomicLong(0);
    private static final Thread                  WRITER;

    static {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }

        WRITER = Thread.ofPlatform()
                .name("async-logger-writer")
                .daemon(true)
                .start(LogDispatcher::writerLoop);
    }

    private LogDispatcher() {}

    static void enqueue(LogEvent event) {
        if (!QUEUE.offer(event)) {
            DROPPED.incrementAndGet();
        }
    }

    static long getDroppedCount() { return DROPPED.get(); }
    static int  getQueueSize()    { return QUEUE.size();  }

    static void shutdown() {
        while (!QUEUE.offer(POISON)) {
            Thread.yield();
        }
        try {
            WRITER.join(10_000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void writerLoop() {
        final LogEvent[] batch = new LogEvent[BATCH_SIZE];
        StringBuilder    sb    = new StringBuilder(4096);
        final Map<String, LoggerState> state = new HashMap<>();
        long lastFlushTime = System.currentTimeMillis();

        try {
            outer:
            while (true) {
                int count = drainBatch(batch);

                for (int i = 0; i < count; i++) {
                    if (batch[i] == POISON) {
                        processBatch(batch, i, sb, state);
                        break outer;
                    }
                }

                if (count == 0) {
                    lastFlushTime = flushAllIfNeeded(state, lastFlushTime);
                    continue;
                }

                processBatch(batch, count, sb, state);

                long now = System.currentTimeMillis();
                if (now - lastFlushTime >= FLUSH_INTERVAL_MS) {
                    flushAll(state);
                    lastFlushTime = now;
                }

                if (sb.capacity() > 1024 * 1024) {
                    sb = new StringBuilder(4096);
                }
            }

            flushAll(state);

        } catch (IOException e) {
            System.err.println("Async logger writer failed: " + e.getMessage());
        } finally {
            state.values().forEach(LoggerState::close);
        }
    }

    private static int drainBatch(LogEvent[] batch) {
        int count = 0;
        try {
            LogEvent first = QUEUE.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
            if (first != null) {
                batch[count++] = first;
                LogEvent next;
                while (count < batch.length && (next = QUEUE.poll()) != null) {
                    batch[count++] = next;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return count;
    }

    private static void processBatch(
            LogEvent[] batch, int count, StringBuilder sb, Map<String, LoggerState> state)
            throws IOException {

        for (int i = 0; i < count; i++) {
            LogEvent event = batch[i];
            batch[i] = null;

            String      name   = event.loggerName();
            LoggerState logger = state.computeIfAbsent(name, LogDispatcher::newLoggerState);

            sb.setLength(0);
            event.appendTo(sb);
            byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

            logger.fileSize += bytes.length;
            writeToBuffer(logger, bytes);

            if (logger.fileSize >= MAX_FILE_SIZE) {
                flushBuffer(logger);
                rotate(logger, name);
            }
        }
    }

    private static final class LoggerState {
        FileChannel channel;
        ByteBuffer  buffer;
        long        fileSize;

        LoggerState(FileChannel channel, long fileSize) {
            this.channel  = channel;
            this.buffer   = ByteBuffer.allocate(BUFFER_SIZE); // heap: allocateDirect offers no benefit at 8 KB
            this.fileSize = fileSize;
        }

        void close() { closeQuietly(channel); }
    }

    private static LoggerState newLoggerState(String loggerName) {
        Path logFile = Paths.get(LOG_DIR, loggerName + ".log");
        long existingSize = 0;
        try {
            if (Files.exists(logFile)) existingSize = Files.size(logFile);
        } catch (IOException ignored) {}
        return new LoggerState(openChannelWithRetry(loggerName), existingSize);
    }

    // Returns the new timestamp when a flush occurred so the caller can advance
    // lastFlushTime. A void method mutating a long parameter would have no effect.
    private static long flushAllIfNeeded(Map<String, LoggerState> state, long lastFlushTime)
            throws IOException {
        long now = System.currentTimeMillis();
        if (now - lastFlushTime >= FLUSH_INTERVAL_MS) {
            flushAll(state);
            return now;
        }
        return lastFlushTime;
    }

    private static void flushAll(Map<String, LoggerState> state) throws IOException {
        for (LoggerState logger : state.values()) {
            if (logger.buffer.position() > 0) {
                flushBuffer(logger);
            }
        }
    }

    private static void writeToBuffer(LoggerState logger, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            if (!logger.buffer.hasRemaining()) {
                flushBuffer(logger);
            }
            int toPut = Math.min(logger.buffer.remaining(), bytes.length - offset);
            logger.buffer.put(bytes, offset, toPut);
            offset += toPut;
        }
    }

    private static void flushBuffer(LoggerState logger) throws IOException {
        logger.buffer.flip();
        while (logger.buffer.hasRemaining()) {
            logger.channel.write(logger.buffer);
        }
        logger.buffer.clear();
    }

    private static void rotate(LoggerState logger, String loggerName) {
        FileChannel oldChannel = logger.channel;
        logger.channel  = null; // null before close so state is never left pointing at a closed channel
        logger.fileSize = 0;
        closeQuietly(oldChannel);

        try {
            rotateFiles(loggerName);
        } catch (IOException e) {
            System.err.println("Log rotation failed for '" + loggerName + "': " + e.getMessage());
        }

        logger.channel = openChannelWithRetry(loggerName);
    }

    private static void rotateFiles(String loggerName) throws IOException {
        Path base = Paths.get(LOG_DIR, loggerName + ".log");
        for (int i = MAX_BACKUPS - 1; i >= 1; i--) {
            Path src = Paths.get(LOG_DIR, loggerName + ".log." + i);
            Path dst = Paths.get(LOG_DIR, loggerName + ".log." + (i + 1));
            if (Files.exists(src)) {
                if (i == MAX_BACKUPS - 1) Files.delete(src);
                else Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (Files.exists(base)) {
            Files.move(base, Paths.get(LOG_DIR, loggerName + ".log.1"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static FileChannel openChannelWithRetry(String loggerName) {
        Path logFile = Paths.get(LOG_DIR, loggerName + ".log");
        while (true) {
            try {
                return FileChannel.open(logFile,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND,
                        StandardOpenOption.WRITE);
            } catch (IOException e) {
                System.err.println("Cannot open log file '" + logFile + "', retrying: " + e.getMessage());
                sleep(1_000);
            }
        }
    }

    private static void closeQuietly(FileChannel channel) {
        if (channel != null) {
            try { channel.close(); } catch (IOException ignored) {}
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}