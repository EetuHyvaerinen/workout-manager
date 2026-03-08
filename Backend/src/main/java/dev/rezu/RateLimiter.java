package dev.rezu;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RateLimiter {

    private static final int BUCKET_CAPACITY = 100;
    private static final long REFILL_INTERVAL_NANOS =
            TimeUnit.MINUTES.toNanos(1) / BUCKET_CAPACITY;

    private static final int MAX_TRACKED_BUCKETS = 10000;
    private static final long STALE_THRESHOLD_NANOS = TimeUnit.MINUTES.toNanos(5);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    public RateLimiter() {
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RateLimitCleaner");
            t.setDaemon(true);
            return t;
        });

        // Background task to remove inactive IPs and free up memory
        this.cleaner.scheduleAtFixedRate(this::pruneStaleBuckets, 5, 5, TimeUnit.MINUTES);
    }

    private static class Bucket {
        private int tokens;
        private long lastRefillTime;
        private volatile long lastAccessTime;

        Bucket(long now) {
            this.tokens = BUCKET_CAPACITY;
            this.lastRefillTime = now;
            this.lastAccessTime = now;
        }
        //synchronized for thread-safety
        synchronized boolean tryConsume(long now) {
            long elapsed = now - lastRefillTime;
            int tokensToAdd = (int) (elapsed / REFILL_INTERVAL_NANOS);

            if (tokensToAdd > 0) {
                tokens = Math.min(BUCKET_CAPACITY, tokens + tokensToAdd);
                lastRefillTime += tokensToAdd * REFILL_INTERVAL_NANOS;
            }

            lastAccessTime = now;

            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        boolean isStale(long now) {
            return now - lastAccessTime > STALE_THRESHOLD_NANOS;
        }
    }

    public boolean allowRequest(String key) {
        final long now = System.nanoTime();
        Bucket bucket = buckets.get(key);

        if (bucket == null) {
            if (buckets.size() >= MAX_TRACKED_BUCKETS && !buckets.containsKey(key)) {
                return false;
            }
            bucket = buckets.computeIfAbsent(key, _ -> new Bucket(now));
        }

        return bucket.tryConsume(now);
    }

    private void pruneStaleBuckets() {
        final long now = System.nanoTime();
        buckets.values().removeIf(b -> b.isStale(now));
    }

    public void shutdown() {
        cleaner.shutdownNow();
    }
}